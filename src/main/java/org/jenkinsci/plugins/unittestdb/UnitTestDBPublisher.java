package org.jenkinsci.plugins.unittestdb;

import java.io.IOException;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.*;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class UnitTestDBPublisher extends Recorder {

  @Extension
  public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl ();
  public static final String PUBLISHER_DISPLAYNAME = "Publish Unit Tests to DB";

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
    public boolean isApplicable ( Class jobType ) {
      return true;
    }

  }
}
