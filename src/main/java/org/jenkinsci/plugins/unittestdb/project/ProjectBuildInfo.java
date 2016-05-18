package org.jenkinsci.plugins.unittestdb.project;

import hudson.model.Action;
import hudson.model.Actionable;
import hudson.model.Api;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.unittestdb.GlobalConfig;
import org.jenkinsci.plugins.unittestdb.db.Failure;
import org.jenkinsci.plugins.unittestdb.db.Job;
import org.jenkinsci.plugins.unittestdb.db.UnitTest;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import javax.persistence.EntityManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

/**
 *
 * @author David van Laatum
 */
@ExportedBean
public class ProjectBuildInfo extends Actionable implements Action {

  private static final Logger LOG
          = Logger.getLogger ( ProjectBuildInfo.class.getName () );

  protected hudson.model.Job<?, ?> project;
  private static final Jenkins JENKINS = Jenkins.getInstance ();

  public ProjectBuildInfo(hudson.model.Job<?, ?> project) {
    this.project = project;
  }

  @Override
  public String getDisplayName () {
    return "Unit Test DB";
  }

  @Override
  public String getIconFileName () {
    return "clipboard.png";
  }

  @Override
  public String getSearchUrl () {
    return null;
  }

  @Override
  public String getUrlName () {
    return "unittestdb";
  }

  @Exported
  public hudson.model.Job<?, ?> getProject() {
    return project;
  }

  public Api getApi () {
    return new Api ( this );
  }

  public boolean hasFailures () {
    return !getFailures ().isEmpty ();
  }

  public boolean hasUnreliable () {
    return !getUnreliable ().isEmpty ();
  }

  @Exported ( inline = true )
  public List<ProjectBuildInfoFailure> getFailures () {
    long begin = System.currentTimeMillis ();
    List<ProjectBuildInfoFailure> failures = new ArrayList<> ();
    GlobalConfig config = requireNonNull ( JENKINS )
            .getInjector ().getInstance ( GlobalConfig.class );
    EntityManager em = null;
    try {
      em = config.getEntityManagerFactory ().createEntityManager ();
      Job job = Job.findByName ( project.getDisplayName (), em, false );
      for ( Failure f : Failure.findByJob ( job, em ).values () ) {
        failures.add ( new ProjectBuildInfoFailure ( f, project, em ) );
      }
    } catch ( SQLException ex ) {
      LOG.log ( Level.SEVERE, null, ex );
    } finally {
      if ( em != null ) {
        em.close ();
      }
    }
    LOG.log ( Level.INFO, "{0} {1}", new Object[]{ project.getName (), System
                                                   .currentTimeMillis () - begin } );
    return failures;
  }

  public Action getFailure ( String id ) {
    Integer failureId = Integer.valueOf ( id );
    ProjectBuildInfoFailure rt = null;
    GlobalConfig config = requireNonNull ( JENKINS )
            .getInjector ().getInstance ( GlobalConfig.class );
    EntityManager em = null;
    try {
      em = config.getEntityManagerFactory ().createEntityManager ();
      rt = new ProjectBuildInfoFailure ( Failure.findByID ( failureId, em ),
                                         project, em );
    } catch ( SQLException ex ) {
      LOG.log ( Level.SEVERE, null, ex );
    } finally {
      if ( em != null ) {
        em.close ();
      }
    }
    return rt;
  }

  public List<ProjectBuildInfoUnreliable> getUnreliable () {
    long begin = System.currentTimeMillis ();
    List<ProjectBuildInfoUnreliable> unreliable = new ArrayList<> ();
    GlobalConfig config = requireNonNull ( JENKINS )
            .getInjector ().getInstance ( GlobalConfig.class );
    EntityManager em = null;
    try {
      em = config.getEntityManagerFactory ().createEntityManager ();
      Job job = Job.findByName ( project.getDisplayName (), em, false );
      List<UnitTest> tests = UnitTest.findUnreliableForJob ( job, em );
      for ( UnitTest test : tests ) {
        unreliable.add ( new ProjectBuildInfoUnreliable ( test ) );
      }
    } catch ( SQLException ex ) {
      LOG.log ( Level.SEVERE, null, ex );
    } finally {
      if ( em != null ) {
        em.close ();
      }
    }
    LOG.log ( Level.INFO, "{0}", System.currentTimeMillis () - begin );
    return unreliable;
  }

}
