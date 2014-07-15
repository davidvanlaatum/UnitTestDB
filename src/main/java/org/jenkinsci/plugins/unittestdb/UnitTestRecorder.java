package org.jenkinsci.plugins.unittestdb;

import com.google.inject.Inject;
import hudson.Launcher;
import hudson.model.*;
import hudson.scm.ChangeLogSet;
import hudson.tasks.test.*;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.unittestdb.DB.*;
import org.jenkinsci.plugins.unittestdb.DB.Build;
import org.jenkinsci.plugins.unittestdb.DB.Failure;
import org.jenkinsci.plugins.unittestdb.DB.Job;
import org.jenkinsci.plugins.unittestdb.DB.Node;
import org.jenkinsci.plugins.unittestdb.DB.User;

import static java.util.Objects.requireNonNull;

/**
 * @author David van Laatum
 */
public class UnitTestRecorder {

  private final Logger LOG
          = Logger.getLogger ( UnitTestRecorder.class.getName () );

  protected AbstractBuild<?, ?> build;
  protected Launcher launcher;
  protected BuildListener listener;
  @Inject
  protected GlobalConfig config;
  protected EntityManager em;
  protected Job job;
  protected Map<String, User> users = new HashMap<> ();
  protected SortedMap<Integer, Failure> failurelist;
  protected SortedMap<String, UnitTest> unittestlist;
  protected Build buildObj;
  protected Node node;
  protected int unittestsProcessed = 0;
  protected BuildInfo buildInfo;

  public UnitTestRecorder ( AbstractBuild<?, ?> build, Launcher launcher,
                            BuildListener listener ) {
    this.build = build;
    this.launcher = launcher;
    this.listener = listener;
    LOG.addHandler ( new JobLogger ( listener.getLogger () ) );
    buildInfo = new BuildInfo ();
  }

  protected void discoverUsers () {
    LOG.log ( Level.INFO, "Found {0} change log entries", build.getChangeSet ()
              .getItems ().length );
    for ( Object i : build.getChangeSet ().getItems () ) {
      if ( i instanceof ChangeLogSet.Entry ) {
        ChangeLogSet.Entry change = (ChangeLogSet.Entry) i;
        User user = User.findByUsername ( change.getAuthor ().getId (), em,
                                          true );
        users.put ( user.getUsername (), user );
      } else if ( i != null ) {
        LOG.log ( Level.WARNING, "Unkown SCM object type {0}", i.getClass ()
                  .getName () );
      }
    }
  }

  protected void discoverUnitTests () {
    AbstractTestResultAction tests = build.getTestResultAction ();
    if ( tests != null ) {
      hudson.tasks.junit.TestResult unittests
              = (hudson.tasks.junit.TestResult) tests.getResult ();

      failurelist = Failure.findByJob ( job, em );
      unittestlist = UnitTest.findByJob ( job, em );

      for ( TestResult test : unittests.getChildren () ) {
        recordUnitTest ( test );
      }
    }
  }

  protected void recordUnitTest ( TestResult test ) {
    if ( test instanceof TabulatedResult && ( (TabulatedResult) test )
            .hasChildren () ) {
      LOG.log ( Level.FINE, "Decending into {0}", test.getFullName () );
      for ( TestResult subtest : ( (TabulatedResult) test )
              .getChildren () ) {
        recordUnitTest ( subtest );
      }
    } else {
      unittestsProcessed++;
      UnitTest t = unittestlist.get ( test.getFullName () );
      if ( t == null ) {
        t = UnitTest.findByJobAndName ( job, test.getFullName (), em, true );
        unittestlist.put ( t.getName (), t );
      }

      BuildUnitTest bt = new BuildUnitTest ();
      bt.setBuild ( buildObj );
      bt.setJob ( job );
      bt.setUnitTest ( t );
      bt.setDuration ( Double.valueOf ( test.getDuration () ) );
      bt.setErrordetails ( test.getErrorDetails () );
      bt.setErrorstack ( test.getErrorStackTrace () );
      if ( test.getBuildResult () == Result.UNSTABLE ) {
        bt.setState ( UnitTestState.Failed );
      } else if ( test.getSkipCount () > 0 ) {
        bt.setState ( UnitTestState.Skipped );
      } else {
        bt.setState ( UnitTestState.Passed );
      }
      bt.setUnitTest ( t );
      em.persist ( bt );

      Failure f = failurelist.get ( requireNonNull ( t.getUnitTestId (),
                                                     "Unit test ID is null" ) );
      if ( bt.getState () == UnitTestState.Failed
                   || bt.getState () == UnitTestState.Error ) {
        if ( f == null ) {
          f = new Failure ();
          f.setFirstBuild ( buildObj );
          f.setLastBuild ( buildObj );
          f.setUnitTest ( t );
          f.setJob ( job );
          f.setState ( FailureState.Failed );
          em.persist ( f );
          for ( User u : users.values () ) {
            FailureUser fu = new FailureUser ();
            fu.setUser ( u );
            fu.setState ( FailureUserState.Maybe );
            fu.setFailure ( f );
            em.persist ( fu );
          }
        }
        f.setLastBuild ( buildObj );
      } else if ( f != null ) {
        f.setLastBuild ( buildObj );
        f.setState ( FailureState.Fixed );
      }
      if ( unittestsProcessed % 1000 == 0 ) {
        LOG.log ( Level.INFO, "Processed {0} unit tests", unittestsProcessed );
      }
    }
  }

  public void record () {
    try {
      Jenkins.getInstance ().getInjector ().injectMembers ( this );
      requireNonNull ( config, "Config is null have you configured me?" );
      em = requireNonNull ( config.getEntityManagerFactory ()
              .createEntityManager (), "Failed to get an EntityManager" );
      job = requireNonNull ( Job.findByName ( build.getParent ()
              .getRootProject ().getName (), em, true ),
                             "Failed to get valid job object!" );
      buildObj = requireNonNull (
              Build.findByJobAndJenkinsID ( job, build.getNumber (), build
                                            .getTime (), em, true ),
              "Failed to get build object" );
      node = requireNonNull ( Node
              .findByName ( build.getBuiltOnStr (), em, true ),
                              "Failed to get Node object" );

      discoverUsers ();
      discoverUnitTests ();

      em.getTransaction ().begin ();
      em.lock ( job, LockModeType.PESSIMISTIC_WRITE );
      job.setLastrun ( build.getTime () );
      job.setLastBuild ( buildObj );
      em.getTransaction ().commit ();

      LOG.log ( Level.INFO, "Processed {0} unit tests and {1} failed",
                new Object[]{ unittestsProcessed,
                              failurelist != null ? failurelist.size () : 0 } );
      build.addAction ( buildInfo );
      for ( Failure f : failurelist.values () ) {
        for ( FailureUser fu : f.getUsers () ) {
          if ( fu.getState () != FailureUserState.Not_Me ) {
            if ( !buildInfo.users.contains ( fu.getUser () ) ) {
              buildInfo.users.add ( fu.getUser () );
            }
          }
        }
      }
      buildInfo.failures.addAll ( failurelist.values () );
    } catch ( SQLException ex ) {
      LOG.log ( Level.SEVERE, null, ex );
    } catch ( NullPointerException ex ) {
      if ( ex.getMessage () != null && !ex.getMessage ().isEmpty () ) {
        throw new IllegalStateException ( ex.getMessage (), ex );
      } else {
        throw ex;
      }
    } finally {
      if ( em != null ) {
        em.close ();
      }
    }
  }

}
