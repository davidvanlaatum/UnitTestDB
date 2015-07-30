package org.jenkinsci.plugins.unittestdb.project;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import hudson.Functions;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixRun;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Actionable;
import hudson.model.Api;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.AggregatedTestResultAction;
import hudson.tasks.test.TestObject;
import hudson.tasks.test.TestResult;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.unittestdb.GlobalConfig;
import org.jenkinsci.plugins.unittestdb.db.BuildUnitTest;
import org.jenkinsci.plugins.unittestdb.db.Failure;
import org.jenkinsci.plugins.unittestdb.db.FailureState;
import org.jenkinsci.plugins.unittestdb.db.FailureUser;
import org.jenkinsci.plugins.unittestdb.db.UnitTestState;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import static java.util.Objects.requireNonNull;
import static org.jenkinsci.plugins.unittestdb.db.FailureState.Gone;
import static org.jenkinsci.plugins.unittestdb.db.FailureUserState.Maybe;

/**
 *
 * @author David van Laatum
 */
@ExportedBean
public class ProjectBuildInfoFailure extends Actionable implements Action {

  private static final Jenkins JENKINS = Jenkins.getInstance ();
  private static final Logger LOG
          = Logger.getLogger ( ProjectBuildInfoFailure.class.getName () );

  protected Integer failureId;
  protected Integer unitTestId;
  protected String name;
  protected TestResult result;
  protected FailureState state;
  protected List<ProjectBuildInfoUser> users;
  protected AbstractBuild<?, ?> firstBuild;
  protected Integer firstBuildId;
  protected AbstractBuild<?, ?> lastBuild;
  protected Integer lastBuildId;
  protected String url;
  protected Double duration;
  protected String errorDetails;
  protected String errorStack;
  protected UnitTestState testState;
  protected AbstractProject<?, ?> project;

  public ProjectBuildInfoFailure ( Failure failure,
                                   AbstractProject<?, ?> project,
                                   EntityManager em ) {
    this.project = project;
    unitTestId = failure.getUnitTest ().getUnitTestId ();
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
      users.add ( new ProjectBuildInfoUser ( fu ) );
    }
    result = findResult ( lastBuild, failure.getUnitTest ().getId (), 0 );
    url = findUrl ( failure, lastBuild, result );
    BuildUnitTest but = BuildUnitTest.findByBuildAndId ( failure
            .getLastBuild (), failure.getUnitTest ().getUnitTestId (), em );
    if ( but != null ) {
      errorDetails = but.getErrorDetails ();
      errorStack = but.getErrorStack ();
      duration = but.getDuration ();
      testState = but.getState ();
    } else {
      LOG.log ( Level.WARNING, "failed to get info about unit test in build" );
    }
  }

  public Api getApi () {
    return new Api ( this );
  }

  public static TestResult findResult ( Object o, String id, int depth ) {
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

  public static String findUrl ( Failure f, AbstractBuild<?, ?> build,
                                 TestResult result ) {
    String rt = null;

    if ( build instanceof MatrixBuild ) {
      for ( MatrixRun r : ( (MatrixBuild) build ).getRuns () ) {
        final AbstractTestResultAction testResultAction
                = r.getAction ( AbstractTestResultAction.class );
        if ( testResultAction != null ) {
          TestResult t = testResultAction.findCorrespondingResult ( f
                  .getUnitTest ()
                  .getId () );
          if ( t != null ) {
            rt = r.getUrl () + testResultAction.getTestResultPath (
                    result );
            break;
          }
        }
      }
    }

    if ( rt == null && build != null && result != null ) {
      rt = build.getUrl () + result.getParentAction ()
              .getTestResultPath ( result );
    }

    return rt;
  }

  /**
   * @return the errorDetails
   */
  @Exported
  public String getErrorDetails () {
    return errorDetails;
  }

  /**
   * @return the errorStack
   */
  @Exported
  public String getErrorStack () {
    return errorStack;
  }

  public AbstractProject<?, ?> getProject () {
    return project;
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

  @Exported
  public Integer getFirstBuildId () {
    return firstBuildId;
  }

  @Exported
  public AbstractBuild<?, ?> getLastBuild () {
    return lastBuild;
  }

  @Exported
  public Integer getLastBuildId () {
    return lastBuildId;
  }

  /**
   * @return the duration
   */
  @Exported
  public Double getDuration () {
    return ( (double) Math.round ( duration * 100 ) ) / 100;
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
  public List<ProjectBuildInfoUser> getUsers () {
    return users;
  }

  @Override
  public String getDisplayName () {
    return name;
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
    GlobalConfig config = JENKINS.getInjector ().getInstance (
            GlobalConfig.class );
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

  protected ProjectBuildInfoUser attachNewUser ( String user ) throws
          SQLException {
    ProjectBuildInfoUser rt = null;
    GlobalConfig config = requireNonNull ( JENKINS )
            .getInjector ().getInstance ( GlobalConfig.class );
    EntityManager em = null;
    try {
      em = config.getEntityManagerFactory ().createEntityManager ();
      org.jenkinsci.plugins.unittestdb.db.User userObj
              = org.jenkinsci.plugins.unittestdb.db.User.findByUsername (
                      user, em, true );
      em.getTransaction ().begin ();
      Failure f = Failure.findByID ( failureId, em );

      FailureUser fu = new FailureUser ();
      fu.setFailure ( f );
      fu.setUser ( userObj );
      fu.setState ( Maybe );
      em.persist ( fu );
      em.getTransaction ().commit ();
      rt = new ProjectBuildInfoUser ( fu );
    } finally {
      if ( em != null ) {
        em.close ();
      }
    }
    return rt;
  }

  public ProjectBuildInfoUser getUser ( String user ) throws SQLException {
    ProjectBuildInfoUser rt = null;
    String userName = user;

    if ( "me".equalsIgnoreCase ( user ) ) {
      hudson.model.User current = hudson.model.User.current ();
      if ( current == null ) {
        throw new IllegalArgumentException ( "Not logged in" );
      }
      userName = current.getId ();
    }

    for ( ProjectBuildInfoUser pbiu : users ) {
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

  @Override
  public ContextMenu doContextMenu ( StaplerRequest request,
                                     StaplerResponse response ) throws Exception {
    ContextMenu menu = super.doContextMenu ( request, response );

    menu.add ( "gone", JENKINS.getRootUrl () + Functions.getResourcePath ()
                               + "/images/16x16/document_delete.png",
               "Mark as Gone",
               true, true );

    return menu;
  }

  @Exported
  public Integer getAge() {
    return lastBuildId - firstBuildId;
  }

  @Exported ( visibility = -1 )
  public List<ProjectBuildInfoRun> getRuns () throws SQLException {
    List<ProjectBuildInfoRun> runs = new ArrayList<> ();
    GlobalConfig config = JENKINS.getInjector ().getInstance (
            GlobalConfig.class );
    EntityManager em = null;
    try {
      em = config.getEntityManagerFactory ().createEntityManager ();
      Query q = em.createNamedQuery ( "BuildUnitTest.findByUnitTestId" );
      q.setParameter ( "unittestid", this.unitTestId );
      q.setMaxResults ( 50 );
      @SuppressWarnings ( "unchecked" )
      List<BuildUnitTest> dbruns = q.getResultList ();
      for ( BuildUnitTest test : dbruns ) {
        runs.add ( new ProjectBuildInfoRun ( project, test ) );
      }
    } finally {
      if ( em != null ) {
        em.close ();
      }
    }
    return runs;
  }

}
