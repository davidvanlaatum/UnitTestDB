package org.jenkinsci.plugins.unittestdb.stats;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.inject.Inject;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import javax.persistence.EntityManager;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.unittestdb.GlobalConfig;
import org.jenkinsci.plugins.unittestdb.JobLogger;
import org.jenkinsci.plugins.unittestdb.db.*;
import org.jenkinsci.plugins.unittestdb.db.Build;
import org.jenkinsci.plugins.unittestdb.db.Job;
import org.kohsuke.stapler.DataBoundConstructor;
import static java.util.Objects.requireNonNull;

/**
 *
 * @author David van Laatum
 */
public class UpdateStats extends Builder {

  private static final Jenkins JENKINS = Jenkins.getInstance ();
  @Extension
  public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl ();
  public static final String BUILDER_DISPLAYNAME
          = "Update Unit Test Statistics";
  protected transient Logger LOG = Logger.getLogger ( UpdateStats.class
          .getName () );
  @Inject
  protected transient GlobalConfig config;

  public Object readResolve () {
    LOG = Logger.getLogger ( UpdateStats.class.getName () );
    return this;
  }

  @DataBoundConstructor
  public UpdateStats () {
  }

  @Override
  public BuildStepDescriptor<Builder> getDescriptor () {
    return DESCRIPTOR;
  }

  @Override
  public boolean perform ( AbstractBuild<?, ?> build, Launcher launcher,
                           BuildListener listener ) throws InterruptedException,
                                                           IOException {
    EntityManager em = null;
    try ( JobLogger jl = new JobLogger ( listener.getLogger (), LOG ) ) {
      JENKINS.getInjector ().injectMembers ( this );
      requireNonNull ( config, "Config is null have you configured me?" );
      em = requireNonNull ( config.getEntityManagerFactory ()
              .createEntityManager (), "Failed to get an EntityManager" );
      em.getTransaction ().begin ();
      calcBuildTotals ( em );
      calcFailureRate ( em );
      em.getTransaction ().commit ();
    } catch ( SQLException ex ) {
      LOG.log ( Level.SEVERE, null, ex );
    } finally {
      if ( em != null && em.isOpen () ) {
        em.close ();
      }
    }
    return true;
  }

  private void calcBuildTotals ( EntityManager em ) {
    List<Build> builds = Build.findAllNeedingStats ( em );
    for ( Build b : builds ) {
      List<BuildUnitTest> buildUnitTestList = b.getBuildUnitTestList ();
      b.setTests ( 0 );
      b.setFailures ( 0 );
      b.setSkipped ( 0 );
      for ( BuildUnitTest test : buildUnitTestList ) {
        b.setTests ( b.getTests () + 1 );
        switch ( test.getState () ) {
          case Error:
          case Failed:
            b.setFailures ( b.getFailures () + 1 );
            break;
          case Passed:
            break;
          case Skipped:
            b.setSkipped ( b.getSkipped () + 1 );
            break;
        }
      }
    }
  }

  class UnitTestInfo {

    public UnitTestInfo ( UnitTest test, UnitTestState previous ) {
      this.test = test;
      this.previous = previous;
    }

    UnitTest test;
    UnitTestState previous;
    int runs = 0;
    int stateChanges = 0;
  }

  private void calcFailureRate ( EntityManager em ) {
    List<Job> jobs = Job.getAll ( em );
    for ( Job job : jobs ) {
      SortedMap<Integer, UnitTestInfo> tests = new TreeMap<> ();
      for ( BuildUnitTest t : job.getLastBuild ().getBuildUnitTestList () ) {
        tests.put ( t.getUnitTest ().getUnitTestId (), new UnitTestInfo ( t
                    .getUnitTest (), t.getState () ) );
      }
      List<Build> builds = job.getBuildList ();
      ListIterator<Build> listIterator = builds.listIterator ( builds.size () );
      int limit = 30;
      while ( listIterator.hasPrevious () ) {
        Build b = listIterator.previous ();
        for ( BuildUnitTest t : b.getBuildUnitTestList () ) {
          UnitTestInfo i = tests.get ( t.getUnitTest ().getUnitTestId () );
          if ( i != null ) {
            i.runs++;
            if ( i.previous != t.getState () ) {
              i.stateChanges++;
            }
            i.previous = t.getState ();
          }
        }
        limit--;
        if ( limit < 0 ) {
          break;
        }
      }

      for ( UnitTestInfo i : tests.values () ) {
        i.test.setRuns ( i.runs );
        i.test.setStatechanges ( i.stateChanges );
        i.test.setFailureRate ( ( i.stateChanges / (double) i.runs ) * 100 );
      }
    }
  }

  public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

    @Override
    public String getDisplayName () {
      return BUILDER_DISPLAYNAME;
    }

    @Override
    public boolean isApplicable ( Class<? extends AbstractProject> jobType ) {
      return FreeStyleProject.class.isAssignableFrom ( jobType );
    }

  }
}
