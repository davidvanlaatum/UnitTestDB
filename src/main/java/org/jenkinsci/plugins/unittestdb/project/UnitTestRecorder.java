package org.jenkinsci.plugins.unittestdb.project;

import com.google.inject.Inject;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogSet;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TabulatedResult;
import hudson.tasks.test.TestResult;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.unittestdb.GlobalConfig;
import org.jenkinsci.plugins.unittestdb.JobLogger;
import org.jenkinsci.plugins.unittestdb.build.BuildInfo;
import org.jenkinsci.plugins.unittestdb.db.*;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

/**
 * @author David van Laatum
 */
public class UnitTestRecorder {

    private static final Jenkins JENKINS = Jenkins.getInstance();
    private final Logger LOG
            = Logger.getLogger(UnitTestRecorder.class.getName());
    protected Run build;
    protected Launcher launcher;
    protected TaskListener listener;
    @Inject
    protected GlobalConfig config;
    protected EntityManager em;
    protected Job job;
    protected Map<String, User> users = new HashMap<>();
    protected SortedMap<Integer, Failure> failurelist;
    protected SortedMap<String, UnitTest> unittestlist;
    protected Build buildObj;
    protected Node node;
    protected int unittestsProcessed = 0;
    protected BuildInfo buildInfo;

    public UnitTestRecorder(Run build, Launcher launcher,
                            TaskListener listener) {
        this.build = build;
        this.launcher = launcher;
        this.listener = listener;
        buildInfo = new BuildInfo(build);
    }

    protected void discoverUsers() {
        LOG.log(Level.INFO, "Discovering users");
        List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSets;
        if (build instanceof AbstractBuild) {
            changeSets = ((AbstractBuild<?, ?>) build).getChangeSets();
        } else if (build instanceof WorkflowRun) {
            changeSets = ((WorkflowRun) build).getChangeSets();
        } else {
            throw new UnsupportedOperationException();
        }
        int count = 0;
        for (ChangeLogSet<? extends ChangeLogSet.Entry> changeSet : changeSets) {
            count += changeSet.getItems().length;
        }
        LOG.log(Level.INFO, "Found {0} change log entries", count);
        for (ChangeLogSet<? extends ChangeLogSet.Entry> changeSet : changeSets) {
            for (Object i : changeSet.getItems()) {
                if (i instanceof ChangeLogSet.Entry) {
                    ChangeLogSet.Entry change = (ChangeLogSet.Entry) i;
                    hudson.model.User u = change.getAuthor();
                    if (u.getProperty(org.jenkinsci.plugins.unittestdb.UserProperty.class).ignoreUser()) {
                        continue;
                    }
                    User user = User.findByUsername(u.getId(), em, true);
                    if (users.put(user.getUsername(), user) == null) {
                        LOG.log(Level.INFO, "Added {0}", user.getUsername());
                    }
                } else if (i != null) {
                    LOG.log(Level.WARNING, "Unknown SCM object type {0}", i.getClass().getName());
                }
            }
        }
        LOG.log(Level.INFO, "Done");
    }

    protected void discoverUnitTests() {
        AbstractTestResultAction tests = build.getAction(
                AbstractTestResultAction.class);
        if (tests != null) {
            if (tests.getResult() instanceof hudson.tasks.junit.TestResult) {
                hudson.tasks.junit.TestResult unittests
                        = (hudson.tasks.junit.TestResult) tests.getResult();

                failurelist = Failure.findByJob(job, em);
                unittestlist = UnitTest.findByJob(job, em);

                for (TestResult test : unittests.getChildren()) {
                    recordUnitTest(test);
                }
            } else if (tests.getResult() != null) {
                LOG.log(Level.WARNING, "Unhandeled type {0}", tests.getResult()
                        .getClass().getName());
            }
        }
    }

    protected void recordUnitTest(TestResult test) {
        if (test instanceof TabulatedResult && ((TabulatedResult) test)
                .hasChildren()) {
            LOG.log(Level.FINE, "Decending into {0}", test.getFullName());
            for (TestResult subtest : ((TabulatedResult) test)
                    .getChildren()) {
                recordUnitTest(subtest);
            }
        } else {
            unittestsProcessed++;
            UnitTest t = unittestlist.get(test.getFullName());
            if (t == null) {
                t = UnitTest.findByJobAndName(job, test.getFullName(), em, true);
                unittestlist.put(t.getName(), t);
            }
            t.setId(test.getId());

            BuildUnitTest bt = new BuildUnitTest();
            bt.setBuild(buildObj);
            bt.setJob(job);
            bt.setUnitTest(t);
            bt.setDuration(Double.valueOf(test.getDuration()));
            bt.setErrorDetails(test.getErrorDetails());
            bt.setErrorStack(test.getErrorStackTrace());
            if (test.getBuildResult() == Result.UNSTABLE) {
                bt.setState(UnitTestState.Failed);
            } else if (test.getSkipCount() > 0) {
                bt.setState(UnitTestState.Skipped);
            } else {
                bt.setState(UnitTestState.Passed);
            }
            bt.setUnitTest(t);
            em.persist(bt);

            Failure f = failurelist.get(requireNonNull(t.getUnitTestId(),
                    "Unit test ID is null"));
            if (bt.getState() == UnitTestState.Failed
                    || bt.getState() == UnitTestState.Error) {
                if (f == null) {
                    f = new Failure();
                    f.setFirstBuild(buildObj);
                    f.setLastBuild(buildObj);
                    f.setUnitTest(t);
                    f.setJob(job);
                    f.setState(FailureState.Failed);
                    em.persist(f);
                    for (User u : users.values()) {
                        FailureUser fu = new FailureUser();
                        fu.setUser(u);
                        fu.setState(FailureUserState.Maybe);
                        fu.setFailure(f);
                        em.persist(fu);
                    }
                }
                f.setLastBuild(buildObj);
            } else if (f != null) {
                f.setLastBuild(buildObj);
                f.setState(FailureState.Fixed);
            }
            if (unittestsProcessed % 1000 == 0) {
                LOG.log(Level.INFO, "Processed {0} unit tests", unittestsProcessed);
            }
        }
    }

    public void record() {
        System.setProperty("javax.persistence.validation.mode", "none");
        try (JobLogger jl = new JobLogger(listener.getLogger(), LOG)) {
            JENKINS.getInjector().injectMembers(this);
            requireNonNull(config, "Config is null have you configured me?");
            em = requireNonNull(config.getEntityManagerFactory()
                    .createEntityManager(), "Failed to get an EntityManager");
            if (build instanceof AbstractBuild) {
                job = requireNonNull(Job.findByName(((AbstractBuild<?, ?>) build).getParent().getRootProject().getFullName(), em, true),
                        "Failed to get valid job object!");
            } else if (build instanceof WorkflowRun) {
                job = requireNonNull(Job.findByName(((WorkflowRun) build).getParent().getFullName(), em, true),
                        "Failed to get valid job object!");
            } else {
                throw new UnsupportedOperationException("Unhandled build class " + build.getClass().getName());
            }
            buildObj = requireNonNull(Build.findByJobAndJenkinsID(job, build.getNumber(), build.getTime(), em, true),
                    "Failed to get build object");
            if (build instanceof AbstractBuild) {
                node = requireNonNull(Node.findByName(((AbstractBuild) build).getBuiltOnStr(), em, true),
                        "Failed to get Node object");
            } else if (build instanceof WorkflowRun) {
                node = requireNonNull(Node.findByName(build.getExecutor().getOwner().getName(), em, true),
                        "Failed to get Node object");
            } else {
                throw new UnsupportedOperationException("Unhandled build class " + build.getClass().getName());
            }

            discoverUsers();
            discoverUnitTests();

            em.getTransaction().begin();
            em.lock(job, LockModeType.PESSIMISTIC_WRITE);
            job.setLastrun(build.getTime());
            job.setLastBuild(buildObj);
            em.getTransaction().commit();

            LOG.log(Level.INFO, "Processed {0} unit tests and {1} failed",
                    new Object[]{unittestsProcessed,
                            failurelist != null ? failurelist.size() : 0});
            build.addAction(buildInfo);
            if (failurelist != null) {
                for (Failure f : failurelist.values()) {
                    buildInfo.addFailure(f);
                }
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } catch (NullPointerException ex) {
            if (ex.getMessage() != null && !ex.getMessage().isEmpty()) {
                throw new IllegalStateException(ex.getMessage(), ex);
            } else {
                throw ex;
            }
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

}
