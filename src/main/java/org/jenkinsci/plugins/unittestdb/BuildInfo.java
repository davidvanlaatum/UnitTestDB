package org.jenkinsci.plugins.unittestdb;

import java.util.ArrayList;
import java.util.List;
import com.google.common.collect.ImmutableList;
import hudson.model.*;
import org.jenkinsci.plugins.unittestdb.db.*;
import org.jenkinsci.plugins.unittestdb.db.Failure;
import static java.util.Objects.requireNonNull;

/**
 * @author David van Laatum
 */
public class BuildInfo extends Actionable implements Action {

  @Override
  public String getSearchUrl () {
    return null;
  }

  public class BIFailureUser {

    protected String username;
    protected FailureUserState state;

    public BIFailureUser ( String username, FailureUserState state ) {
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

  public class BIFailure {

    protected FailureState state;
    protected String unitTestName;
    protected List<BIFailureUser> users;
    protected int firstBuild;
    protected int lastBuild;

    public BIFailure ( FailureState state, String unitTestName,
                       List<BIFailureUser> users, int firstBuild, int lastBuild ) {
      this.state = state;
      this.unitTestName = unitTestName;
      this.users = users;
      this.firstBuild = firstBuild;
      this.lastBuild = lastBuild;
    }

    public FailureState getState () {
      return state;
    }

    public List<BIFailureUser> getUsers () {
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

  private final List<String> users = new ArrayList<> ();
  private final List<BIFailure> failures = new ArrayList<> ();
  private final AbstractBuild<?, ?> build;

  public BuildInfo ( AbstractBuild<?, ?> build ) {
    this.build = build;
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
  public String getUrlName () {
    return "unittestdb";
  }

  public boolean hasFailures () {
    return !failures.isEmpty ();
  }

  public List<String> getUsers () {
    return ImmutableList.copyOf ( users );
  }

  public void addFailure ( Failure failure ) {
    requireNonNull ( failure );
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
    requireNonNull ( failure.getFirstBuild () );
    requireNonNull ( failures );
    failures.add ( new BIFailure ( failure.getState (),
                                   failure.getUnitTest ().getName (), fuser,
                                   failure.getFirstBuild () != null ? failure
                                   .getFirstBuild ().getJenkinsId () : null,
                                   failure.getLastBuild ().getJenkinsId () ) );
  }

  public AbstractBuild<?, ?> getBuild () {
    return build;
  }

  public List<BIFailure> getFailures () {
    return ImmutableList.copyOf ( failures );
  }
}
