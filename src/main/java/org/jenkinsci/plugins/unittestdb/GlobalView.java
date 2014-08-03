package org.jenkinsci.plugins.unittestdb;

import org.jenkinsci.plugins.unittestdb.project.ProjectBuildInfo;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import hudson.Extension;
import hudson.Util;
import hudson.model.*;
import javax.persistence.EntityManager;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.unittestdb.db.Job;
import org.kohsuke.stapler.export.Exported;
import static java.util.Objects.requireNonNull;

/**
 * @author David van Laatum
 */
@Extension
public class GlobalView extends Actionable implements RootAction {

  private static final Logger LOG
          = Logger.getLogger ( GlobalView.class.getName () );

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

  public Api getApi () {
    return new Api ( this );
  }

  @Exported ( inline = true )
  public Collection<ProjectBuildInfo> getJobs () {
    SortedMap<String, ProjectBuildInfo> jobs = new TreeMap<> ();
    GlobalConfig config = requireNonNull ( Jenkins.getInstance () )
            .getInjector ().getInstance ( GlobalConfig.class );
    EntityManager em = null;
    try {
      em = config.getEntityManagerFactory ().createEntityManager ();
      List<TopLevelItem> projects = Util.createSubList ( Jenkins
              .getInstance ().getAllItems ( AbstractProject.class ),
                                                         TopLevelItem.class );
      for ( Job job : Job.getAll ( em ) ) {
        AbstractProject<?, ?> project = null;

        for ( TopLevelItem item : projects ) {
          if ( item instanceof AbstractProject ) {
            if ( job.getName ().equals ( item.getName () ) ) {
              project = (AbstractProject<?, ?>) item;
              break;
            }
          }
        }

        if ( project != null ) {
          ProjectBuildInfo info = new ProjectBuildInfo ( project );
          jobs.put ( job.getName (), info );
        }
      }
    } catch ( SQLException ex ) {
      LOG.log ( Level.SEVERE, null, ex );
    } finally {
      if ( em != null ) {
        em.close ();
      }
    }

    return jobs.values ();
  }

}
