package org.jenkinsci.plugins.unittestdb.reminder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.common.collect.ImmutableList;
import hudson.model.User;
import javax.mail.*;
import javax.mail.internet.*;
import jenkins.model.Jenkins;
import org.apache.commons.jelly.JellyContext;
import org.apache.commons.jelly.JellyException;
import org.apache.commons.jelly.Script;
import org.apache.commons.jelly.XMLOutput;
import org.jenkinsci.plugins.unittestdb.db.Failure;
import org.xml.sax.InputSource;

/**
 *
 * @author David van Laatum
 */
public class UserData {

  private final Logger LOG
          = Logger.getLogger ( UserData.class.getName () );

  String username;
  InternetAddress email;
  SortedMap<String, EMailJob> tests = new TreeMap<> ();
  SortedMap<String, EMailJob> unclaimed = new TreeMap<> ();

  public UserData ( String username, Logger parentLog ) {
    LOG.setParent ( parentLog );
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

  void setUnclaimed ( SortedMap<String, List<Failure>> unclaimed ) {
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
