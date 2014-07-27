package org.jenkinsci.plugins.unittestdb;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixRun;
import hudson.model.*;
import hudson.model.User;
import hudson.tasks.test.*;
import javax.persistence.EntityManager;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.unittestdb.db.*;
import org.jenkinsci.plugins.unittestdb.db.Failure;
import org.jenkinsci.plugins.unittestdb.db.Job;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import static java.util.Objects.requireNonNull;
import static org.jenkinsci.plugins.unittestdb.db.FailureState.Gone;
import static org.jenkinsci.plugins.unittestdb.db.FailureUserState.*;

/**
 *
 * @author David van Laatum
 */
@ExportedBean
public class ProjectBuildInfo extends Actionable implements Action {

  @ExportedBean
  public class PBIUser extends Actionable implements Action {

    protected String username;
    protected FailureUserState state;
    protected PBIFailure failure;
    protected Integer id;

    public PBIUser ( FailureUser fu ) {
      username = fu.getUser ().getUsername ();
      state = fu.getState ();
      id = fu.getFailureUserId ();
    }

    @Override
    public String getDisplayName () {
      return null;
    }

    @Override
    public String getIconFileName () {
      return null;
    }

    @Override
    public String getSearchUrl () {
      return null;
    }

    /**
     * @return the state
     */
    @Exported
    public FailureUserState getState () {
      return state;
    }

    @Override
    public String getUrlName () {
      return null;
    }

    /**
     * @return the username
     */
    public String getUsername () {
      return username;
    }

    @Exported ( inline = true )
    public hudson.model.User getUser () {
      return requireNonNull ( Jenkins.getInstance () ).getUser ( username );
    }

    protected void doUpdateTo ( FailureUserState state ) throws SQLException {
      GlobalConfig config = requireNonNull ( Jenkins.getInstance () )
              .getInjector ().getInstance ( GlobalConfig.class );
      EntityManager em = null;
      try {
        em = config.getEntityManagerFactory ().createEntityManager ();
        em.getTransaction ().begin ();
        FailureUser fu = FailureUser.findByID ( id, em );
        fu.setState ( state );
        em.getTransaction ().commit ();
      } finally {
        if ( em != null ) {
          em.close ();
        }
      }
    }

    public void doWasme ( StaplerRequest req, StaplerResponse rsp ) throws
            SQLException, ServletException, IOException {
      doUpdateTo ( Was_Me );
      rsp.forwardToPreviousPage ( req );
    }

    public void doNotme ( StaplerRequest req, StaplerResponse rsp ) throws
            SQLException, ServletException, IOException {
      doUpdateTo ( Not_Me );
      rsp.forwardToPreviousPage ( req );
    }

    public void doMaybe ( StaplerRequest req, StaplerResponse rsp ) throws
            SQLException, ServletException, IOException {
      doUpdateTo ( Maybe );
      rsp.forwardToPreviousPage ( req );
    }

    public void doMightbeme ( StaplerRequest req, StaplerResponse rsp ) throws
            SQLException, ServletException, IOException {
      doUpdateTo ( Might_be_Me );
      rsp.forwardToPreviousPage ( req );
    }

  }

  @ExportedBean
  public class PBIFailure extends Actionable implements Action {

    protected Integer failureId;
    protected String name;
    protected TestResult result;
    protected FailureState state;
    protected List<PBIUser> users;
    protected AbstractBuild<?, ?> firstBuild;
    protected Integer firstBuildId;
    protected AbstractBuild<?, ?> lastBuild;
    protected Integer lastBuildId;
    protected String url;
    protected Double duration;
    protected UnitTestState testState;

    public PBIFailure ( Failure failure, AbstractProject<?, ?> project,
                        EntityManager em ) {
      failureId = failure.getFailureId ();
      name = failure.getUnitTest ().getName ();
      state = failure.getState ();
      firstBuildId = failure.getFirstBuild ().getJenkinsId ();
      lastBuildId = failure.getLastBuild ().getJenkinsId ();
      firstBuild = project.getBuildByNumber ( firstBuildId );
      lastBuild = project.getBuildByNumber ( lastBuildId );
      duration = null;
      testState = null;
      users = new ArrayList<> ();
      for ( FailureUser fu : failure.getUsers () ) {
        users.add ( new PBIUser ( fu ) );
      }
      result = findResult ( lastBuild, failure.getUnitTest ().getId (), 0 );
      url = findUrl ( failure );
      BuildUnitTest but = BuildUnitTest.findByBuildAndId ( failure
              .getLastBuild (), failure.getUnitTest ().getUnitTestId (), em );
      if ( but != null ) {
        duration = but.getDuration ();
        testState = but.getState ();
      } else {
        LOG.log ( Level.WARNING, "failed to get info about unit test in build" );
      }
    }

    private TestResult findResult ( Object o, String id, int depth ) {
      TestResult rt = null;
      if ( depth > 10 || o == null ) {
        // do nothing
      } else if ( o instanceof TestObject ) {
        rt = ( (TestObject) o ).findCorrespondingResult ( id );
      } else if ( o instanceof List ) {
        for ( Object l : (List) o ) {
          rt = findResult ( l, id, depth + 1 );
          if ( rt != null ) {
            break;
          }
        }
      } else if ( o instanceof AggregatedTestResultAction.ChildReport ) {
        rt = findResult ( ( (AggregatedTestResultAction.ChildReport) o ).result,
                          id, depth + 1 );
      } else if ( o instanceof AbstractBuild ) {
        rt = findResult ( ( (AbstractBuild) o ).getTestResultAction (), id,
                          depth + 1 );
        if ( rt == null ) {
          rt = findResult ( ( (AbstractBuild) o )
                  .getAggregatedTestResultAction (), id, depth + 1 );
        }
        if ( rt == null ) {
          if ( o instanceof MatrixBuild ) {
            for ( MatrixRun r : ( (MatrixBuild) o ).getRuns () ) {
              rt = findResult ( r.getTestResultAction (), id, depth + 1 );
              if ( rt != null ) {
                break;
              }
            }
          }
        }
      } else if ( o instanceof AbstractTestResultAction ) {
        rt = findResult ( ( (AbstractTestResultAction) o ).getResult (), id,
                          depth + 1 );
      } else {
        Class c = o.getClass ().getSuperclass ();
        LOG.log ( Level.WARNING, "Unhandled type {0}", c.getName () );
        while ( c != null ) {
          LOG.log ( Level.WARNING, "Parent is {0}", c.getName () );
          c = c.getSuperclass ();
        }
      }
      return rt;
    }

    private String findUrl ( Failure f ) {
      String rt = null;

      if ( lastBuild instanceof MatrixBuild ) {
        for ( MatrixRun r : ( (MatrixBuild) lastBuild ).getRuns () ) {
          TestResult t = r.getTestResultAction ().findCorrespondingResult ( f
                  .getUnitTest ()
                  .getId () );
          if ( t != null ) {
            rt = r.getUrl () + r.getTestResultAction ().getTestResultPath (
                    result );
            break;
          }
        }
      }

      if ( rt == null && lastBuild != null && result != null ) {
        rt = lastBuild.getUrl () + result.getParentAction ()
                .getTestResultPath ( result );
      }

      return rt;
    }

    @Exported
    public String getUrl () {
      return url;
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
     * @return the duration
     */
    @Exported
    public Double getDuration () {
      return duration;
    }

    @Exported
    public UnitTestState getTestState () {
      return testState;
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

    @Override
    public String getDisplayName () {
      return null;
    }

    @Override
    public String getIconFileName () {
      return null;
    }

    @Override
    public String getSearchUrl () {
      return null;
    }

    @Override
    public String getUrlName () {
      return null;
    }

    public void doGone ( StaplerRequest req, StaplerResponse rsp ) throws
            SQLException, ServletException, IOException {
      GlobalConfig config = requireNonNull ( Jenkins.getInstance () )
              .getInjector ().getInstance ( GlobalConfig.class );
      EntityManager em = null;
      try {
        em = config.getEntityManagerFactory ().createEntityManager ();
        em.getTransaction ().begin ();
        Failure f = Failure.findByID ( failureId, em );
        f.setState ( Gone );
        em.getTransaction ().commit ();
      } finally {
        if ( em != null ) {
          em.close ();
        }
      }
      rsp.forwardToPreviousPage ( req );
    }

    protected PBIUser attachNewUser ( String user ) throws SQLException {
      PBIUser rt = null;
      GlobalConfig config = requireNonNull ( Jenkins.getInstance () )
              .getInjector ().getInstance ( GlobalConfig.class );
      EntityManager em = null;
      try {
        em = config.getEntityManagerFactory ().createEntityManager ();
        em.getTransaction ().begin ();
        Failure f = Failure.findByID ( failureId, em );

        FailureUser fu = new FailureUser ();
        fu.setFailure ( f );
        fu.setUser ( org.jenkinsci.plugins.unittestdb.db.User.findByUsername (
                user, em, true ) );
        fu.setState ( Maybe );
        em.persist ( fu );
        em.getTransaction ().commit ();
        rt = new PBIUser ( fu );
      } finally {
        if ( em != null ) {
          em.close ();
        }
      }
      return rt;
    }

    public PBIUser getUser ( String user ) throws SQLException {
      PBIUser rt = null;
      String userName = user;

      if ( "me".equalsIgnoreCase ( user ) ) {
        userName = requireNonNull ( User.current (), "Not logged in" ).getId ();
      }

      for ( PBIUser pbiu : users ) {
        if ( pbiu.getUsername ().equals ( userName ) ) {
          rt = pbiu;
          break;
        }
      }

      if ( rt == null ) {
        rt = attachNewUser ( userName );
      }

      return rt;
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
        rt.add ( new PBIFailure ( f, project, em ) );
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
