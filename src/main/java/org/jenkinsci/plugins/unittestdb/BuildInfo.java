package org.jenkinsci.plugins.unittestdb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.google.common.collect.ImmutableList;
import hudson.model.Action;
import org.jenkinsci.plugins.unittestdb.db.Failure;
import org.jenkinsci.plugins.unittestdb.db.FailureState;
import org.jenkinsci.plugins.unittestdb.db.FailureUser;
import org.jenkinsci.plugins.unittestdb.db.FailureUserState;

/**
 * @author David van Laatum
 */
public class BuildInfo implements Action {

  public class BIFailureUser implements Serializable {

    protected String username;
    protected FailureUserState state;

    public BIFailureUser ( String username, FailureUserState state ) {
      this.username = username;
      this.state = state;
    }

  }

  public class BIFailure implements Serializable {

    protected FailureState state;
    protected String unitTestName;
    protected List<BIFailureUser> users;

    public BIFailure ( FailureState state, String unitTestName,
                       List<BIFailureUser> users ) {
      this.state = state;
      this.unitTestName = unitTestName;
      this.users = users;
    }
  }

  private final List<String> users = new ArrayList<> ();
  private final List<BIFailure> failures = new ArrayList<> ();

  @Override
  public String getDisplayName () {
    return "Unit Test DB";
  }

  @Override
  public String getIconFileName () {
    return null;
  }

  @Override
  public String getUrlName () {
    return null;
  }

  public boolean hasFailures () {
    return !failures.isEmpty ();
  }

  public List<String> getUsers () {
    return ImmutableList.copyOf ( users );
  }

  public void addFailure ( Failure failure ) {
    List<BIFailureUser> fuser = new ArrayList<> ();
    for ( FailureUser fu : failure.getUsers () ) {
      fuser.add ( new BIFailureUser ( fu.getUser ().getUsername (), fu
                                      .getState () ) );
      if ( fu.getState () != FailureUserState.Not_Me ) {
        if ( !users.contains ( fu.getUser ().getUsername () ) ) {
          users.add ( fu.getUser ().getUsername () );
        }
      }
    }
    failures.add ( new BIFailure ( failure.getState (), failure.getUnitTest ()
                                   .getName (), fuser ) );
  }
}
