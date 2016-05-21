package org.jenkinsci.plugins.unittestdb.reminder;

import hudson.model.Job;
import hudson.model.Run;
import hudson.model.User;
import hudson.tasks.test.TestResult;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.unittestdb.db.Failure;
import org.jenkinsci.plugins.unittestdb.db.FailureUser;
import org.jenkinsci.plugins.unittestdb.project.ProjectBuildInfoFailure;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author David van Laatum
 */
public class EMailTest {

  private static final Jenkins JENKINS = Jenkins.getInstance ();

  protected String name;
  protected Run<?, ?> firstBuild;
  protected Run<?, ?> lastBuild;
  protected List<EMailUser> users = new ArrayList<> ();
  protected String url;
  protected TestResult result;

  public EMailTest(Failure f, Job<?, ?> project) {
    name = f.getUnitTest ().getName ();
    firstBuild = project.getBuildByNumber(f.getFirstBuild().getJenkinsId());
    lastBuild = project.getBuildByNumber ( f.getLastBuild ().getJenkinsId () );
    for ( FailureUser u : f.getUsers () ) {
      User user = JENKINS.getUser ( u.getUser ().getUsername () );
      users.add ( new EMailUser ( user, u.getState () ) );
    }
    result = ProjectBuildInfoFailure.findResult ( lastBuild, f.getUnitTest ().getId (), 0 );
    if ( result != null ) {
      url = ProjectBuildInfoFailure.findUrl ( f, lastBuild, result );
    }
  }

  public String getName () {
    return name;
  }

  public Run<?, ?> getFirstBuild() {
    return firstBuild;
  }

  public Run<?, ?> getLastBuild() {
    return lastBuild;
  }

  public List<EMailUser> getUsers () {
    return users;
  }

  public String getUrl () {
    return url;
  }

  public TestResult getResult () {
    return result;
  }

}
