package org.jenkinsci.plugins.unittestdb;

import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.model.User;
import hudson.tasks.*;
import hudson.tasks.test.TestResult;
import javax.mail.*;
import javax.mail.internet.*;
import javax.persistence.EntityManager;
import jenkins.model.Jenkins;
import jenkins.model.JenkinsLocationConfiguration;
import org.apache.commons.jelly.*;
import org.jenkinsci.plugins.unittestdb.db.*;
import org.jenkinsci.plugins.unittestdb.db.Failure;
import org.jenkinsci.plugins.unittestdb.db.Job;
import org.kohsuke.stapler.DataBoundConstructor;
import org.xml.sax.InputSource;
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
                ud = new UserData ( user.getUser ().getUsername () );
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
      return true;
    }

  }

  public class EMailUser {

    protected User user;
    protected FailureUserState state;

    public EMailUser ( User user, FailureUserState state ) {
      this.user = user;
      this.state = state;
    }

    public String getName () {
      return user.getDisplayName ();
    }

    public FailureUserState getState () {
      return state;
    }
  }

  public class EMailTest {

    protected String name;
    protected AbstractBuild<?, ?> firstBuild;
    protected AbstractBuild<?, ?> lastBuild;
    protected List<EMailUser> users = new ArrayList<> ();
    protected String url;
    protected TestResult result;

    public EMailTest ( Failure f, AbstractProject<?, ?> project ) {
      name = f.getUnitTest ().getName ();
      firstBuild = project
              .getBuildByNumber ( f.getFirstBuild ().getJenkinsId () );
      lastBuild = project.getBuildByNumber ( f.getLastBuild ().getJenkinsId () );
      for ( FailureUser u : f.getUsers () ) {
        User user = JENKINS.getUser ( u.getUser ().getUsername () );
        users.add ( new EMailUser ( user, u.getState () ) );
      }
      result = PBIFailure.findResult ( lastBuild, f.getUnitTest ().getId (), 0 );
      if ( result != null ) {
        url = PBIFailure.findUrl ( f, lastBuild, result );
      }
    }

    public String getName () {
      return name;
    }

    public AbstractBuild<?, ?> getFirstBuild () {
      return firstBuild;
    }

    public AbstractBuild<?, ?> getLastBuild () {
      return lastBuild;
    }

    public List<EMailUser> getUsers () {
      return users;
    }

    public String getUrl () {
      return url;
    }

    public TestResult getResult () {
      return result;
    }

  }

  public class EMailJob {

    protected AbstractProject<?, ?> project;
    protected List<EMailTest> tests = new ArrayList<> ();

    public EMailJob ( Job job ) {
      project = AbstractProject.findNearest ( job.getName () );
    }

    public void add ( Failure f ) {
      tests.add ( new EMailTest ( f, project ) );
    }

    public String getName () {
      return project.getName ();
    }

    public List<EMailTest> getTests () {
      return tests;
    }

    public AbstractProject<?, ?> getProject () {
      return project;
    }

  }

  public class UserData {

    String username;
    InternetAddress email;
    SortedMap<String, EMailJob> tests = new TreeMap<> ();
    SortedMap<String, EMailJob> unclaimed = new TreeMap<> ();

    public UserData ( String username ) {
      this.username = username;
      User jenkinsUser = User.get ( username, true, null );
      hudson.tasks.Mailer.UserProperty jemail = jenkinsUser.getProperty (
              hudson.tasks.Mailer.UserProperty.class );
      if ( jemail != null ) {
        String address = jemail.getAddress ();
        if ( address != null && !address.isEmpty () ) {
          try {
            this.email = new InternetAddress ( address );
            this.email.setPersonal ( jenkinsUser.getDisplayName () );
          } catch ( AddressException | UnsupportedEncodingException ex ) {
            LOG.log ( Level.SEVERE,
                      "Exception while fetching email address for user "
                              + jenkinsUser, ex );
          }
        }
      }
    }

    public String getUsername () {
      return username;
    }

    public List<EMailJob> getJobs () {
      return ImmutableList.copyOf ( tests.values () );
    }

    public List<EMailJob> getUnclaimed () {
      return ImmutableList.copyOf ( unclaimed.values () );
    }

    private void setUnclaimed ( SortedMap<String, List<Failure>> unclaimed ) {
      for ( List<Failure> fl : unclaimed.values () ) {
        for ( Failure f : fl ) {
          EMailJob job = this.unclaimed.get ( f.getJob ().getName () );
          if ( job == null ) {
            job = new EMailJob ( f.getJob () );
            this.unclaimed.put ( job.getName (), job );
          }
          job.add ( f );
        }
      }
    }

    public void add ( Failure f ) {
      LOG.log ( Level.INFO, "Adding failure {0}:{1} for user {2}", new Object[]{
        f.getJob ().getName (), f.getUnitTest ().getName (), username } );
      EMailJob job = tests.get ( f
              .getJob ().getName () );
      if ( job == null ) {
        job = new EMailJob ( f.getJob () );
        tests.put ( f.getJob ().getName (), job );
        LOG.log ( Level.INFO, "Created job {0}", f.getJob ().getName () );
      }
      job.add ( f );
    }

    private JellyContext createContext () {
      JellyContext context = new JellyContext ();
      context.setVariable ( "app", Jenkins.getInstance () );
      context.setVariable ( "it", this );
      return context;
    }

    private Script compileScript ( JellyContext context ) throws JellyException,
                                                                 IOException {
      try ( InputStream file = getClass ().getResourceAsStream (
              "reminderemail.jelly" ) ) {
        return context.compileScript ( new InputSource ( file ) );
      }
    }

    private String buildContent () {
      try ( ByteArrayOutputStream output = new ByteArrayOutputStream () ) {
        JellyContext context = createContext ();
        Script script = compileScript ( context );
        XMLOutput xmlOutput = XMLOutput.createXMLOutput ( output );
        script.run ( context, xmlOutput );
        xmlOutput.flush ();
        xmlOutput.close ();
        return output.toString ();
      } catch ( JellyException | IOException ex ) {
        LOG.log ( Level.SEVERE, null, ex );
        return null;
      }
    }

    public void sendNotify ( Session mailsession, InternetAddress from ) {
      if ( email != null ) {
        LOG.log ( Level.INFO, "Sending list of broken tests to {0}", email );
        Transport transport = null;
        try {
          MimeBodyPart body = new MimeBodyPart ();
          body.setContent ( buildContent (), "text/html" );

          MimeMultipart content = new MimeMultipart ();
          content.addBodyPart ( body );

          MimeMessage message = new MimeMessage ( mailsession );
          message.setSubject ( "Unit Tests you may have broken" );
          message.setRecipient ( Message.RecipientType.TO, email );
          message.setContent ( content );
          if ( from != null ) {
            message.setFrom ( from );
          } else {
            message.setFrom ();
          }
          transport = mailsession.getTransport ( email );
          transport.connect ();
          transport.sendMessage ( message, new Address[]{ email } );
        } catch ( MessagingException ex ) {
          LOG.log ( Level.SEVERE, null, ex );
        } finally {
          try {
            if ( transport != null ) {
              transport.close ();
            }
          } catch ( MessagingException ex ) {
            LOG.log ( Level.SEVERE, null, ex );
          }
        }
      } else {
        LOG.log ( Level.INFO, "No email address for user {0}", username );
      }
    }

  }
}
