package org.jenkinsci.plugins.unittestdb.build;

import java.util.ArrayList;
import java.util.List;
import com.google.common.collect.ImmutableList;
import hudson.init.Initializer;
import hudson.model.*;
import org.jenkinsci.plugins.unittestdb.db.*;
import org.jenkinsci.plugins.unittestdb.db.Failure;
import org.kohsuke.stapler.export.ExportedBean;
import static hudson.init.InitMilestone.PLUGINS_STARTED;
import static java.util.Objects.requireNonNull;

/**
 * @author David van Laatum
 */
@ExportedBean
public class BuildInfo extends Actionable implements Action {

  @Override
  public String getSearchUrl () {
    return null;
  }

  @Initializer ( before = PLUGINS_STARTED )
  public static void addAliases () {
    Run.XSTREAM2.addCompatibilityAlias (
            "org.jenkinsci.plugins.unittestdb.BuildInfo", BuildInfo.class );
    Run.XSTREAM2.addCompatibilityAlias (
            "org.jenkinsci.plugins.unittestdb.BuildInfo$BIFailure",
            BuildInfoFailure.class );
    Run.XSTREAM2.addCompatibilityAlias (
            "org.jenkinsci.plugins.unittestdb.BuildInfo$BIFailureUser",
            BuildInfoFailureUser.class );
  }

  private final List<String> users = new ArrayList<> ();
  private final List<BuildInfoFailure> failures = new ArrayList<> ();
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
    List<BuildInfoFailureUser> fuser = new ArrayList<> ();
    for ( FailureUser fu : failure.getUsers () ) {
      fuser.add ( new BuildInfoFailureUser ( fu.getUser ().getUsername (), fu
                                             .getState () ) );
      if ( fu.getState () != FailureUserState.Not_Me ) {
        if ( !users.contains ( fu.getUser ().getUsername () ) ) {
          users.add ( fu.getUser ().getUsername () );
        }
      }
    }
    requireNonNull ( failure.getFirstBuild () );
    requireNonNull ( failures );
    String name = failure.getUnitTest ().getName ();
    Integer firstBuild = failure.getFirstBuild () != null ? failure
            .getFirstBuild ().getJenkinsId () : null;
    Integer lastBuild = failure.getLastBuild ().getJenkinsId ();
    failures.add ( new BuildInfoFailure ( failure.getState (), name, fuser,
                                          firstBuild, lastBuild ) );
  }

  public AbstractBuild<?, ?> getBuild () {
    return build;
  }

  public List<BuildInfoFailure> getFailures () {
    return ImmutableList.copyOf ( failures );
  }
}
