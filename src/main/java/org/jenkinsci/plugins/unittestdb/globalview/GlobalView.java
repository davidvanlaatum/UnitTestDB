package org.jenkinsci.plugins.unittestdb.globalview;

import hudson.Extension;
import hudson.Util;
import hudson.model.Actionable;
import hudson.model.Api;
import hudson.model.RootAction;
import hudson.model.TopLevelItem;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.unittestdb.GlobalConfig;
import org.jenkinsci.plugins.unittestdb.db.Job;
import org.jenkinsci.plugins.unittestdb.project.ProjectBuildInfo;
import org.kohsuke.stapler.export.Exported;

import javax.persistence.EntityManager;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author David van Laatum
 */
@Extension
public class GlobalView extends Actionable implements RootAction {

  private static final Jenkins JENKINS = Jenkins.getInstance ();
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
    GlobalConfig config = JENKINS.getInjector ().getInstance (
            GlobalConfig.class );
    EntityManager em = null;
    try {
      em = config.getEntityManagerFactory ().createEntityManager ();
      List<TopLevelItem> projects = Util.createSubList(JENKINS.getAllItems(), TopLevelItem.class);
      for ( Job job : Job.getAll ( em ) ) {
        hudson.model.Job<?, ?> project = null;

        for ( TopLevelItem item : projects ) {
          if (item instanceof hudson.model.Job) {
            if (job.getName().equals(item.getFullName())) {
              project = (hudson.model.Job<?, ?>) item;
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
