package org.jenkinsci.plugins.unittestdb;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import hudson.model.*;
import hudson.tasks.test.TestResult;
import javax.persistence.EntityManager;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.unittestdb.db.*;
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

  @ExportedBean
  public class PBIUser {

    protected String username;
    protected FailureUserState state;

    public PBIUser ( FailureUser fu ) {
      username = fu.getUser ().getUsername ();
      state = fu.getState ();
    }

    /**
     * @return the state
     */
    @Exported
    public FailureUserState getState () {
      return state;
    }

    /**
     * @return the username
     */
    public String getUsername () {
      return username;
    }

    @Exported ( inline = true )
    public hudson.model.User getUser () {
      return Jenkins.getInstance ().getUser ( username );
    }

  }

  @ExportedBean
  public class PBIFailure {

    protected Integer failureId;
    protected String name;
    protected TestResult result;
    protected FailureState state;
    protected List<PBIUser> users;
    protected AbstractBuild<?, ?> firstBuild;
    protected Integer firstBuildId;
    protected AbstractBuild<?, ?> lastBuild;
    protected Integer lastBuildId;

    public PBIFailure ( Failure failure, AbstractProject<?, ?> project ) {
      failureId = failure.getFailureId ();
      name = failure.getUnitTest ().getName ();
      state = failure.getState ();
      firstBuildId = failure.getFirstBuild ().getJenkinsId ();
      lastBuildId = failure.getLastBuild ().getJenkinsId ();
      firstBuild = project.getBuildByNumber ( firstBuildId );
      lastBuild = project.getBuildByNumber ( lastBuildId );
      users = new ArrayList<> ();
      for ( FailureUser fu : failure.getUsers () ) {
        users.add ( new PBIUser ( fu ) );
      }

      if ( lastBuild != null ) {
        if ( lastBuild.getTestResultAction () != null ) {
          result = lastBuild.getTestResultAction ()
                  .findCorrespondingResult ( failure.getUnitTest ().getId () );
        }
      }
    }

    @Exported
    public Integer getFailureId () {
      return failureId;
    }

    @Exported
    public AbstractBuild<?, ?> getFirstBuild () {
      return firstBuild;
    }

    public Integer getFirstBuildId () {
      return firstBuildId;
    }

    @Exported
    public AbstractBuild<?, ?> getLastBuild () {
      return lastBuild;
    }

    public Integer getLastBuildId () {
      return lastBuildId;
    }

    /**
     * @return the name
     */
    @Exported
    public String getName () {
      return name;
    }

    /**
     * @return the result
     */
    public TestResult getResult () {
      return result;
    }

    /**
     * @return the state
     */
    @Exported
    public FailureState getState () {
      return state;
    }

    /**
     * @return the users
     */
    @Exported ( inline = true )
    public List<PBIUser> getUsers () {
      return users;
    }

  }

  private static final Logger LOG
          = Logger.getLogger ( ProjectBuildInfo.class.getName () );

  protected AbstractProject<?, ?> project;

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

  @Exported ( inline = true )
  public List<PBIFailure> getFailures () {
    List<PBIFailure> rt = new ArrayList<> ();
    GlobalConfig config = requireNonNull ( Jenkins.getInstance () )
            .getInjector ().getInstance ( GlobalConfig.class );
    EntityManager em = null;
    try {
      em = config.getEntityManagerFactory ().createEntityManager ();
      Job job = Job.findByName ( project.getDisplayName (), em, false );
      for ( Failure f : Failure.findByJob ( job, em ).values () ) {
        rt.add ( new PBIFailure ( f, project ) );
      }
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
