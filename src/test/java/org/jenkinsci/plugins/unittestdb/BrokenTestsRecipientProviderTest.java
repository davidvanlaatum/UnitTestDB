package org.jenkinsci.plugins.unittestdb;

import hudson.EnvVars;
import hudson.console.ConsoleNote;
import hudson.model.*;
import hudson.model.queue.SubTask;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.ExtendedEmailPublisherContext;
import hudson.util.AbstractTaskListener;
import java.io.*;
import java.util.*;
import javax.mail.internet.InternetAddress;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.*;

/**
 *
 * @author David van Laatum
 */
public class BrokenTestsRecipientProviderTest {

  public BrokenTestsRecipientProviderTest () {
  }

  @Rule
  public JenkinsRule j = new JenkinsRule ();

  /**
   * Test of addRecipients method, of class BrokenTestsRecipientProvider.
   */
  @Test
  public void testAddRecipients () throws Exception {
    BrokenTestsRecipientProvider obj = new BrokenTestsRecipientProvider ();
    final ByteArrayOutputStream out = new ByteArrayOutputStream ();
    TaskListener listener = new AbstractTaskListener () {
      PrintStream log = new PrintStream ( out );

      @Override
      public void annotate ( ConsoleNote ann ) throws IOException {
        throw new UnsupportedOperationException ( "Not supported yet." );
      }

      @Override
      public PrintWriter error ( String msg ) {
        throw new UnsupportedOperationException ( "Not supported yet." );
      }

      @Override
      public PrintWriter error ( String format, Object... args ) {
        throw new UnsupportedOperationException ( "Not supported yet." );
      }

      @Override
      public PrintWriter fatalError ( String msg ) {
        throw new UnsupportedOperationException ( "Not supported yet." );
      }

      @Override
      public PrintWriter fatalError ( String format, Object... args ) {
        throw new UnsupportedOperationException ( "Not supported yet." );
      }

      @Override
      public PrintStream getLogger () {
        return log;
      }
    };

    FreeStyleProject project = j.createFreeStyleProject ();
    j.buildAndAssertSuccess ( project );
    ExtendedEmailPublisher extemail = new ExtendedEmailPublisher ();
    FreeStyleBuild build = project.getLastBuild ();
    ExtendedEmailPublisherContext context = new ExtendedEmailPublisherContext (
            extemail, build, listener );
    EnvVars vars = new EnvVars ();
    Set<InternetAddress> to = new HashSet<> ();
    Set<InternetAddress> cc = new HashSet<> ();
    Set<InternetAddress> bcc = new HashSet<> ();

    obj.addRecipients ( context, vars, to, cc, bcc );

    assertTrue ( to.isEmpty () );
    assertTrue ( cc.isEmpty () );
    assertTrue ( bcc.isEmpty () );
    assertEquals ( "INFO: No info from Unit Test Publisher\n", out.toString () );

    out.reset ();

    BuildInfo buildInfo = new BuildInfo ();
    buildInfo.users = new ArrayList<> ();
    buildInfo.failures = new ArrayList<> ();

    build.addAction ( buildInfo );

    obj.addRecipients ( context, vars, to, cc, bcc );

    assertTrue ( to.isEmpty () );
    assertTrue ( cc.isEmpty () );
    assertTrue ( bcc.isEmpty () );
    assertEquals ( "", out.toString () );

    org.jenkinsci.plugins.unittestdb.DB.User u
            = new org.jenkinsci.plugins.unittestdb.DB.User ();
    u.setUsername ( "testuser" );
    buildInfo.users.add ( u );

    obj.addRecipients ( context, vars, to, cc, bcc );

    assertTrue ( to.isEmpty () );
    assertTrue ( cc.isEmpty () );
    assertTrue ( bcc.isEmpty () );
    assertEquals ( "INFO: No email address for user testuser\n", out.toString () );

    out.reset ();

    assertNotNull ( j.getInstance ().getUser ( "testuser" ) );
    j.getInstance ().getUser ( "testuser" ).addProperty (
            new hudson.tasks.Mailer.UserProperty ( "test@test.com" ) );
    j.getInstance ().getUser ( "testuser" ).setFullName ( "A Test User" );

    obj.addRecipients ( context, vars, to, cc, bcc );

    assertFalse ( to.isEmpty () );
    assertTrue ( cc.isEmpty () );
    assertTrue ( bcc.isEmpty () );
    assertEquals ( "INFO: Added test@test.com to list of recipients\n", out
                   .toString () );
    assertEquals ( 1, to.size () );

    assertEquals ( "test@test.com", to.iterator ().next ().getAddress () );
    assertEquals ( "A Test User", to.iterator ().next ().getPersonal () );
  }

}
