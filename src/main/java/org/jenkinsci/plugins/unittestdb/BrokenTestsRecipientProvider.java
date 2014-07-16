package org.jenkinsci.plugins.unittestdb;

import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import hudson.EnvVars;
import hudson.Extension;
import hudson.plugins.emailext.ExtendedEmailPublisherContext;
import hudson.plugins.emailext.plugins.RecipientProvider;
import hudson.plugins.emailext.plugins.RecipientProviderDescriptor;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.unittestdb.DB.User;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author David van Laatum
 */
public class BrokenTestsRecipientProvider extends RecipientProvider {

  @Extension
  public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl ();
  public static final String DISPLAYNAME = "Unit Test Breakers";

  @DataBoundConstructor
  public BrokenTestsRecipientProvider () {
  }

  @Override
  public void addRecipients ( ExtendedEmailPublisherContext context, EnvVars env,
                              Set<InternetAddress> to, Set<InternetAddress> cc,
                              Set<InternetAddress> bcc ) {
    Logger LOG = Logger.getLogger ( BrokenTestsRecipientProvider.class
            .getName () );
    try ( JobLogger jobLogger = new JobLogger ( context.getListener ()
            .getLogger (), LOG ) ) {
      BuildInfo info = context.getBuild ().getAction ( BuildInfo.class );
      if ( info != null ) {
        for ( User u : info.users ) {
          boolean found = false;
          hudson.model.User user = Jenkins.getInstance ()
                  .getUser ( u.getUsername () );
          if ( user != null ) {
            hudson.tasks.Mailer.UserProperty email = user.getProperty (
                    hudson.tasks.Mailer.UserProperty.class );
            if ( email != null ) {
              String address = email.getAddress ();
              if ( address != null && !address.isEmpty () ) {
                found = true;
                try {
                  InternetAddress a = new InternetAddress ( address );
                  a.setPersonal ( user.getDisplayName () );
                  to.add ( a );
                  LOG.log ( Level.INFO, "Added {0} to list of recipients",
                            address );
                } catch ( AddressException | UnsupportedEncodingException ex ) {
                  LOG.log ( Level.SEVERE,
                            "Exception while adding email address for user " + u
                            .getUsername (), ex );
                }
              }
            }
          }
          if ( !found ) {
            LOG.log ( Level.INFO, "No email address for user {0}", u
                      .getUsername () );
          }
        }
      } else {
        LOG.log ( Level.INFO, "No info from Unit Test Publisher" );
      }
    }
  }

  @Override
  public RecipientProviderDescriptor getDescriptor () {
    return DESCRIPTOR;
  }

  public static class DescriptorImpl extends RecipientProviderDescriptor {

    @Override
    public String getDisplayName () {
      return DISPLAYNAME;
    }

  }

}
