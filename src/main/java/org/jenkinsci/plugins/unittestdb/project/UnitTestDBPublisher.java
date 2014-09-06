package org.jenkinsci.plugins.unittestdb.project;

import java.io.IOException;
import java.util.Collection;
import com.google.common.collect.ImmutableList;
import hudson.Extension;
import hudson.Launcher;
import hudson.init.Initializer;
import hudson.matrix.MatrixProject;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.model.Items;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import static hudson.init.InitMilestone.PLUGINS_STARTED;

public class UnitTestDBPublisher extends Recorder {

  @Override
  public Collection<? extends Action> getProjectActions (
          AbstractProject<?, ?> project ) {
    return ImmutableList.of ( new ProjectBuildInfo ( project ) );
  }

  @Extension
  public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl ();
  public static final String PUBLISHER_DISPLAYNAME = "Publish Unit Tests to DB";

  @Initializer ( before = PLUGINS_STARTED )
  public static void addAliases () {
    Items.XSTREAM2.addCompatibilityAlias (
            "org.jenkinsci.plugins.unittestdb.UnitTestDBPublisher",
            UnitTestDBPublisher.class );
    Items.XSTREAM2.addCompatibilityAlias (
            "org.jenkinsci.plugins.unittestdb.ProjectBuildInfo",
            ProjectBuildInfo.class );
    Items.XSTREAM2.addCompatibilityAlias (
            "org.jenkinsci.plugins.unittestdb.PBIUser",
            ProjectBuildInfoUser.class );
    Items.XSTREAM2.addCompatibilityAlias (
            "org.jenkinsci.plugins.unittestdb.PBIFailure",
            ProjectBuildInfoFailure.class );
  }

  @DataBoundConstructor
  public UnitTestDBPublisher () {
    super ();
  }

  @Override
  public BuildStepMonitor getRequiredMonitorService () {
    return BuildStepMonitor.NONE;
  }

  @Override
  public boolean perform ( AbstractBuild<?, ?> build, Launcher launcher,
                           BuildListener listener ) throws InterruptedException,
                                                           IOException {
    UnitTestRecorder recorder
            = new UnitTestRecorder ( build, launcher, listener );
    recorder.record ();
    return true;
  }

  @Override
  public BuildStepDescriptor<Publisher> getDescriptor () {
    return DESCRIPTOR;
  }

  public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

    @Override
    public boolean configure ( StaplerRequest req, JSONObject json ) throws
            FormException {
      return super.configure ( req, json );
    }

    @Override
    public String getDisplayName () {
      return PUBLISHER_DISPLAYNAME;
    }

    @Override
    public boolean isApplicable (
            Class<? extends AbstractProject> jobType ) {
      if ( jobType.getName ().equals (
              "org.jenkinsci.plugins.DistributedTests.DistributedProject" ) ) {
        return true;
      }
      return FreeStyleProject.class.isAssignableFrom ( jobType )
                     || MatrixProject.class.isAssignableFrom ( jobType );
    }

  }
}
