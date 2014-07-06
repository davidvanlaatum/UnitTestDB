package org.jenkinsci.plugins.unittestdb;

import com.google.inject.Inject;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.database.Database;
import org.jenkinsci.plugins.database.jpa.PersistenceService;
import org.jenkinsci.plugins.unittestdb.DB.DBObject;
import org.jenkinsci.plugins.unittestdb.DB.Job;

import static java.util.Objects.requireNonNull;

/**
 * @author David van Laatum
 */
public class UnitTestBuildCreator {

  private final Logger LOG
          = Logger.getLogger ( UnitTestBuildCreator.class.getName () );

  protected AbstractBuild<?, ?> build;
  protected BuildListener listener;
  @Inject
  GlobalConfig config;
  @Inject
  PersistenceService ps;

  public UnitTestBuildCreator ( AbstractBuild<?, ?> build,
                                BuildListener listener ) {
    this.build = build;
    this.listener = listener;
    LOG.addHandler ( new JobLogger ( listener.getLogger () ) );
  }

  public void create () {
    EntityManager em = null;
    try {
      Jenkins.getInstance ().getInjector ().injectMembers ( this );
      requireNonNull ( config, "Config is null have you configured me?" );
      em = requireNonNull ( config.getEntityManagerFactory ()
              .createEntityManager (), "Failed to get an EntityManager" );
      Job job = Job.findByName ( build.getParent ().getRootProject ()
              .getName (), em, true );
      if ( job == null ) {
        throw new IllegalStateException ( "Failed to get valid job object!" );
      }

      LOG.log ( Level.INFO, "Job has id {0}", job.getJobId () );
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
