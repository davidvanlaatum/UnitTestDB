package org.jenkinsci.plugins.unittestdb;

import java.util.ArrayList;
import java.util.List;
import hudson.model.Action;
import org.jenkinsci.plugins.unittestdb.DB.Failure;
import org.jenkinsci.plugins.unittestdb.DB.User;

/**
 * @author David van Laatum
 */
public class BuildInfo implements Action {

  public List<User> users = new ArrayList<> ();
  public List<Failure> failures = new ArrayList<> ();

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
}
