package org.jenkinsci.plugins.unittestdb.reminder;

import hudson.model.User;
import org.jenkinsci.plugins.unittestdb.db.FailureUserState;

/**
 *
 * @author David van Laatum
 */
public class EMailUser {

  protected User user;
  protected FailureUserState state;

  public EMailUser ( User user, FailureUserState state ) {
    this.user = user;
    this.state = state;
  }

  public String getName () {
    return user.getDisplayName ();
  }

  public FailureUserState getState () {
    return state;
  }
}
