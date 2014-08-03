package org.jenkinsci.plugins.unittestdb.build;

import java.util.List;
import com.google.common.collect.ImmutableList;
import org.jenkinsci.plugins.unittestdb.db.FailureState;

/**
 *
 * @author David van Laatum
 */
public class BuildInfoFailure {

  protected FailureState state;
  protected String unitTestName;
  protected List<BuildInfoFailureUser> users;
  protected int firstBuild;
  protected int lastBuild;

  public BuildInfoFailure ( FailureState state, String unitTestName,
                     List<BuildInfoFailureUser> users, int firstBuild, int lastBuild ) {
    this.state = state;
    this.unitTestName = unitTestName;
    this.users = users;
    this.firstBuild = firstBuild;
    this.lastBuild = lastBuild;
  }

  public FailureState getState () {
    return state;
  }

  public List<BuildInfoFailureUser> getUsers () {
    return ImmutableList.copyOf ( users );
  }

  public String getUnitTestName () {
    return unitTestName;
  }

  public int getFirstBuild () {
    return firstBuild;
  }

  public int getLastBuild () {
    return lastBuild;
  }

}
