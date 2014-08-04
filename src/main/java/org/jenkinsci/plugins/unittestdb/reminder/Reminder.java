package org.jenkinsci.plugins.unittestdb.reminder;

import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.inject.Inject;
import hudson.Extension;
import hudson.Launcher;
import hudson.init.Initializer;
import hudson.model.*;
import hudson.tasks.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.persistence.EntityManager;
import jenkins.model.Jenkins;
import jenkins.model.JenkinsLocationConfiguration;
import org.jenkinsci.plugins.unittestdb.*;
import org.jenkinsci.plugins.unittestdb.db.*;
import org.jenkinsci.plugins.unittestdb.db.Failure;
import org.kohsuke.stapler.DataBoundConstructor;
import static hudson.init.InitMilestone.PLUGINS_STARTED;
import static java.util.Objects.*;
import static org.jenkinsci.plugins.unittestdb.db.FailureUserState.Not_Me;

/**
 * @author David van Laatum
 */
public class Reminder extends Builder {

  private static final Jenkins JENKINS = Jenkins.getInstance ();
  private static final Logger LOG
          = Logger.getLogger ( Reminder.class.getName () );
  @Extension
  public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl ();
  public static final String BUILDER_DISPLAYNAME
          = "Send reminders about broken tests";
  @Inject
  protected GlobalConfig config;

  @DataBoundConstructor
  public Reminder () {
  }

  @Override
  public BuildStepDescriptor<Builder> getDescriptor () {
    return DESCRIPTOR;
  }

  @Initializer ( before = PLUGINS_STARTED )
  public static void addAliases () {
    Items.XSTREAM2.addCompatibilityAlias (
            "org.jenkinsci.plugins.unittestdb.Reminder", Reminder.class );
  }

  @Override
  public boolean perform ( AbstractBuild<?, ?> build, Launcher launcher,
                           BuildListener listener ) throws InterruptedException,
                                                           IOException {
    EntityManager em = null;
    try ( JobLogger jl = new JobLogger ( listener.getLogger (), LOG ) ) {
      JENKINS.getInjector ().injectMembers ( this );
      requireNonNull ( config, "Config is null have you configured me?" );
      em = requireNonNull ( config.getEntityManagerFactory ()
              .createEntityManager (), "Failed to get an EntityManager" );

      List<Failure> list = Failure.findByState ( FailureState.Failed, em );
      if ( list != null ) {
        LOG.log ( Level.INFO, "Found {0} failures", list.size () );

        SortedMap<String, UserData> users = new TreeMap<> ();
        SortedMap<String, List<Failure>> unclaimed = new TreeMap<> ();

        for ( Failure failure : list ) {
          LOG.log ( Level.FINE, "Failure for {0} in job {1}", new Object[]{
            failure.getUnitTest ().getName (), failure.getJob ().getName () } );

          boolean hasUsers = false;
          for ( FailureUser user : failure.getUsers () ) {
            if ( user.getState () != Not_Me ) {
              UserData ud = users.get ( user.getUser ().getUsername () );
              if ( ud == null ) {
                ud = new UserData ( user.getUser ().getUsername (), LOG );
                users.put ( user.getUser ().getUsername (), ud );
              }
              ud.add ( failure );
              hasUsers = true;
            }
          }

          if ( !hasUsers ) {
            LOG.log ( Level.WARNING, "{0} in {1} is not claimed", new Object[]{
              failure.getUnitTest ().getName (), failure.getJob ().getName () } );
            List<Failure> fl = unclaimed.get ( failure.getJob ().getName () );
            if ( fl == null ) {
              fl = new ArrayList<> ();
              unclaimed.put ( failure.getJob ().getName (), fl );
            }
            fl.add ( failure );
          }
        }

        Session mailsession = Mailer.descriptor ().createSession ();
        InternetAddress from = null;
        if ( Mailer.descriptor ().getReplyToAddress () != null ) {
          from = new InternetAddress ( Mailer.descriptor ()
                  .getReplyToAddress () );
        } else if ( JenkinsLocationConfiguration.get ().getAdminAddress ()
                            != null ) {
          from = new InternetAddress ( JenkinsLocationConfiguration.get ()
                  .getAdminAddress () );
        }

        for ( UserData user : users.values () ) {
          user.setUnclaimed ( unclaimed );
          user.sendNotify ( mailsession, from );
        }
      } else {
        LOG.log ( Level.INFO, "No failures found" );
      }
    } catch ( AddressException | SQLException ex ) {
      LOG.log ( Level.SEVERE, null, ex );
    } finally {
      if ( em != null && em.isOpen () ) {
        em.close ();
      }
    }
    return true;
  }

  public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

    @Override
    public String getDisplayName () {
      return BUILDER_DISPLAYNAME;
    }

    @Override
    public boolean isApplicable (
            Class<? extends AbstractProject> jobType ) {
      return FreeStyleProject.class.isAssignableFrom ( jobType );
    }

  }

}
