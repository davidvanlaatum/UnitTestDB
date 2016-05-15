package org.jenkinsci.plugins.unittestdb.email;

import hudson.Extension;
import hudson.init.Initializer;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixRun;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.model.Items;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;
import hudson.plugins.emailext.plugins.RecipientProvider;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.unittestdb.build.BuildInfo;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.util.ArrayList;
import java.util.List;

import static hudson.init.InitMilestone.PLUGINS_STARTED;

/**
 *
 * @author David van Laatum
 */
public class BrokenTestsTrigger extends EmailTrigger {

  @DataBoundConstructor
  public BrokenTestsTrigger ( List<RecipientProvider> recipientProviders,
                              String recipientList, String replyTo,
                              String subject, String body,
                              String attachmentsPattern,
                              int attachBuildLog, String contentType ) {
    super ( recipientProviders, recipientList, replyTo, subject, body,
            attachmentsPattern, attachBuildLog, contentType );
  }

  public BrokenTestsTrigger ( JSONObject formData ) {
    super ( formData );
  }

  @Initializer ( before = PLUGINS_STARTED )
  public static void addAliases () {
    Items.XSTREAM2.addCompatibilityAlias (
            "org.jenkinsci.plugins.unittestdb.BrokenTestsTrigger",
            BrokenTestsTrigger.class );
  }

  @Extension
  public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl ();
  public static final String DISPLAYNAME = "Broken Unit Tests";

  @Override
  public boolean trigger ( AbstractBuild<?, ?> build, TaskListener listener ) {
    boolean rt = false;
    BuildInfo info = build.getAction ( BuildInfo.class );
    if ( info != null ) {
      rt = info.hasFailures ();
    }
    if ( !rt && build instanceof MatrixBuild ) {
      for ( MatrixRun run : ( (MatrixBuild) build ).getRuns () ) {
        info = run.getAction ( BuildInfo.class );
        if ( info != null ) {
          rt = info.hasFailures ();
          if ( rt ) {
            break;
          }
        }
      }
    }
    return rt;
  }

  @Override
  public EmailTriggerDescriptor getDescriptor () {
    return DESCRIPTOR;
  }

  public static class DescriptorImpl extends EmailTriggerDescriptor {

    @Override
    public boolean configure ( StaplerRequest req, JSONObject json ) throws
            Descriptor.FormException {
      return super.configure ( req, json );
    }

    @Override
    public String getDisplayName () {
      return DISPLAYNAME;
    }

    @Override
    public EmailTrigger createDefault() {
      return new BrokenTestsTrigger(new ArrayList<RecipientProvider>(), null, null, null, null, null, 0, null);
    }
  }

}
