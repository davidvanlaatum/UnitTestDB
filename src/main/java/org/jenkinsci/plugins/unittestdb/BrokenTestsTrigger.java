package org.jenkinsci.plugins.unittestdb;

import hudson.Extension;
import hudson.model.*;
import hudson.plugins.emailext.plugins.*;
import java.util.List;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

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

  @Extension
  public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl ();
  public static final String DISPLAYNAME = "Broken Unit Tests";

  @Override
  public boolean trigger ( AbstractBuild<?, ?> build, TaskListener listener ) {
    boolean rt = false;
    BuildInfo info = build.getAction ( BuildInfo.class );
    if ( info != null ) {
      rt = !info.failures.isEmpty ();
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

  }

}
