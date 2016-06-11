package org.jenkinsci.plugins.unittestdb.email;

import hudson.EnvVars;
import hudson.Extension;
import hudson.init.Initializer;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixRun;
import hudson.model.Items;
import hudson.plugins.emailext.ExtendedEmailPublisherContext;
import hudson.plugins.emailext.plugins.RecipientProvider;
import hudson.plugins.emailext.plugins.RecipientProviderDescriptor;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.unittestdb.JobLogger;
import org.jenkinsci.plugins.unittestdb.build.BuildInfo;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static hudson.init.InitMilestone.PLUGINS_STARTED;

/**
 * @author David van Laatum
 */
public class BrokenTestsRecipientProvider extends RecipientProvider {

  private static final Jenkins JENKINS = Jenkins.getInstance ();
  @Extension
  public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl ();
  public static final String DISPLAYNAME = "Unit Test Breakers";

  @Initializer ( before = PLUGINS_STARTED )
  public static void addAliases () {
    Items.XSTREAM2.addCompatibilityAlias (
            "org.jenkinsci.plugins.unittestdb.BrokenTestsRecipientProvider",
            BrokenTestsRecipientProvider.class );
  }

  @DataBoundConstructor
  public BrokenTestsRecipientProvider () {
  }

  @Override
  public void addRecipients ( ExtendedEmailPublisherContext context, EnvVars env,
                              Set<InternetAddress> to, Set<InternetAddress> cc,
                              Set<InternetAddress> bcc ) {
    Logger LOG = Logger.getLogger ( BrokenTestsRecipientProvider.class.getName () );
    try ( JobLogger jobLogger = new JobLogger ( context.getListener ().getLogger (), LOG ) ) {
      BuildInfo info = context.getRun ().getAction ( BuildInfo.class );
      if ( info != null ) {
        for ( String u : info.getUsers () ) {
          addUserTo ( u, to, LOG );
        }
      } else {
        LOG.log ( Level.INFO, "No info from Unit Test Publisher" );
      }

      if ( context.getRun () instanceof MatrixBuild ) {
        for ( MatrixRun run : ( (MatrixBuild) context.getRun () ).getRuns () ) {
          info = run.getAction ( BuildInfo.class );
          if ( info != null ) {
            for ( String u : info.getUsers () ) {
              addUserTo ( u, to, LOG );
            }
          } else {
            LOG.log ( Level.INFO, "No info from Unit Test Publisher" );
          }
        }
      }
    }
  }

  private void addUserTo ( String u, Set<InternetAddress> to, Logger LOG ) {
    boolean found = false;
    hudson.model.User user = JENKINS.getUser ( u );
    if ( user != null ) {
      hudson.tasks.Mailer.UserProperty email = user.getProperty ( hudson.tasks.Mailer.UserProperty.class );
      if ( email != null ) {
        String address = email.getAddress ();
        if ( address != null && !address.isEmpty () ) {
          found = true;
          try {
            InternetAddress a = new InternetAddress ( address );
            a.setPersonal ( user.getDisplayName () );
            to.add ( a );
            LOG.log ( Level.INFO, "Added {0} to list of recipients", address );
          } catch ( AddressException | UnsupportedEncodingException ex ) {
            LOG.log ( Level.SEVERE, "Exception while adding email address for user " + u, ex );
          }
        }
      }
    }
    if ( !found ) {
      LOG.log ( Level.INFO, "No email address for user {0}", u );
    }
  }

  @Override
  public RecipientProviderDescriptor getDescriptor () {
    return DESCRIPTOR;
  }

  public static class DescriptorImpl extends RecipientProviderDescriptor {

    @Override
    @Nonnull
    public String getDisplayName () {
      return DISPLAYNAME;
    }

  }

}
