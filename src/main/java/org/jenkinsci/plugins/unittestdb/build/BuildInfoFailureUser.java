package org.jenkinsci.plugins.unittestdb.build;

import org.jenkinsci.plugins.unittestdb.db.FailureUserState;

/**
 *
 * @author David van Laatum
 */
public class BuildInfoFailureUser {

  protected String username;
  protected FailureUserState state;

  public BuildInfoFailureUser ( String username, FailureUserState state ) {
    this.username = username;
    this.state = state;
  }

  public FailureUserState getState () {
    return state;
  }

  public String getUsername () {
    return username;
  }

}
