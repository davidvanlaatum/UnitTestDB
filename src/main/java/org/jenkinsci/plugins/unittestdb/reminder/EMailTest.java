package org.jenkinsci.plugins.unittestdb.reminder;

import java.util.ArrayList;
import java.util.List;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.User;
import hudson.tasks.test.TestResult;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.unittestdb.db.Failure;
import org.jenkinsci.plugins.unittestdb.db.FailureUser;
import org.jenkinsci.plugins.unittestdb.project.ProjectBuildInfoFailure;

/**
 *
 * @author David van Laatum
 */
public class EMailTest {

  private static final Jenkins JENKINS = Jenkins.getInstance ();

  protected String name;
  protected AbstractBuild<?, ?> firstBuild;
  protected AbstractBuild<?, ?> lastBuild;
  protected List<EMailUser> users = new ArrayList<> ();
  protected String url;
  protected TestResult result;

  public EMailTest ( Failure f, AbstractProject<?, ?> project ) {
    name = f.getUnitTest ().getName ();
    firstBuild = project
            .getBuildByNumber ( f.getFirstBuild ().getJenkinsId () );
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

  public AbstractBuild<?, ?> getFirstBuild () {
    return firstBuild;
  }

  public AbstractBuild<?, ?> getLastBuild () {
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
