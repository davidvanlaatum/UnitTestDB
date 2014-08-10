package org.jenkinsci.plugins.unittestdb.project;

import hudson.model.AbstractProject;
import hudson.model.Api;
import org.jenkinsci.plugins.unittestdb.db.BuildUnitTest;
import org.jenkinsci.plugins.unittestdb.db.UnitTestState;
import org.kohsuke.stapler.export.Exported;

/**
 * @author David van Laatum
 */
public class ProjectBuildInfoRun {

  protected AbstractProject<?, ?> project;
  protected UnitTestState testState;
  protected Double duration;
  protected Integer jenkinsBuildId;

  public ProjectBuildInfoRun ( AbstractProject<?, ?> project, BuildUnitTest run ) {
    this.project = project;
    this.testState = run.getState ();
    this.duration = run.getDuration ();
    this.jenkinsBuildId = run.getBuild ().getJenkinsId ();
  }

  public Api getApi () {
    return new Api ( this );
  }

  /**
   * @return the duration
   */
  @Exported
  public Double getDuration () {
    return ( (double) Math.round ( duration * 100 ) ) / 100;
  }

  /**
   * @return the jenkinsBuildId
   */
  public Integer getJenkinsBuildId () {
    return jenkinsBuildId;
  }

  /**
   * @return the project
   */
  public AbstractProject<?, ?> getProject () {
    return project;
  }

  /**
   * @return the testState
   */
  @Exported
  public UnitTestState getTestState () {
    return testState;
  }

}
