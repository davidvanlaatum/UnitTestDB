package org.jenkinsci.plugins.unittestdb.project;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import hudson.model.*;
import javax.persistence.EntityManager;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.unittestdb.GlobalConfig;
import org.jenkinsci.plugins.unittestdb.db.Failure;
import org.jenkinsci.plugins.unittestdb.db.Job;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import static java.util.Objects.requireNonNull;

/**
 *
 * @author David van Laatum
 */
@ExportedBean
public class ProjectBuildInfo extends Actionable implements Action {

  private static final Logger LOG
          = Logger.getLogger ( ProjectBuildInfo.class.getName () );

  protected AbstractProject<?, ?> project;
  protected List<PBIFailure> failures;

  public ProjectBuildInfo ( AbstractProject<?, ?> project ) {
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
  public AbstractProject<?, ?> getProject () {
    return project;
  }

  public Api getApi () {
    return new Api ( this );
  }

  public boolean hasFailures () {
    return !getFailures ().isEmpty ();
  }

  @Exported ( inline = true )
  public List<PBIFailure> getFailures () {
    if ( failures == null ) {
      failures = new ArrayList<> ();
      GlobalConfig config = requireNonNull ( Jenkins.getInstance () )
              .getInjector ().getInstance ( GlobalConfig.class );
      EntityManager em = null;
      try {
        em = config.getEntityManagerFactory ().createEntityManager ();
        Job job = Job.findByName ( project.getDisplayName (), em, false );
        for ( Failure f : Failure.findByJob ( job, em ).values () ) {
          failures.add ( new PBIFailure ( f, project, em ) );
        }
      } catch ( SQLException ex ) {
        LOG.log ( Level.SEVERE, null, ex );
      } finally {
        if ( em != null ) {
          em.close ();
        }
      }
    }
    return failures;
  }

  public Action getFailure ( String id ) {
    Integer failureId = Integer.valueOf ( id );
    PBIFailure rt = null;
    GlobalConfig config = requireNonNull ( Jenkins.getInstance () )
            .getInjector ().getInstance ( GlobalConfig.class );
    EntityManager em = null;
    try {
      em = config.getEntityManagerFactory ().createEntityManager ();
      rt = new PBIFailure ( Failure.findByID ( failureId, em ), project, em );
    } catch ( SQLException ex ) {
      LOG.log ( Level.SEVERE, null, ex );
    } finally {
      if ( em != null ) {
        em.close ();
      }
    }
    return rt;
  }

}