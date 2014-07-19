package org.jenkinsci.plugins.unittestdb;

import java.io.*;
import java.util.*;
import com.google.common.collect.ImmutableList;
import hudson.EnvVars;
import hudson.console.ConsoleNote;
import hudson.model.*;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.ExtendedEmailPublisherContext;
import hudson.util.AbstractTaskListener;
import javax.mail.internet.InternetAddress;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.unittestdb.db.UnitTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import static org.junit.Assert.*;

/**
 * @author David van Laatum
 */
@RunWith ( PowerMockRunner.class )
@PrepareForTest ( { Jenkins.class } )
public class BrokenTestsRecipientProviderTest {

  /**
   * Test of addRecipients method, of class BrokenTestsRecipientProvider.
   *
   * @throws java.lang.Exception
   */
  @Test
  public void testAddRecipients () throws Exception {
    final String TESTUSER = "testuser";
    final String TESTEMAIL = "test@test.com";
    final String TESTFULLNAME = "A Test User";
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

    Jenkins j = PowerMockito.mock ( Jenkins.class );
    PowerMockito.mockStatic ( Jenkins.class );
    PowerMockito.when ( Jenkins.getInstance () ).thenReturn ( j );

    AbstractBuild build = PowerMockito.mock ( AbstractBuild.class );
    ExtendedEmailPublisher extemail = new ExtendedEmailPublisher ();
    ExtendedEmailPublisherContext context = new ExtendedEmailPublisherContext (
            extemail, build, listener );
    EnvVars vars = new EnvVars ();
    Set<InternetAddress> to = new HashSet<> ();
    Set<InternetAddress> cc = new HashSet<> ();
    Set<InternetAddress> bcc = new HashSet<> ();

    BrokenTestsRecipientProvider obj = new BrokenTestsRecipientProvider ();

    obj.addRecipients ( context, vars, to, cc, bcc );

    assertTrue ( to.isEmpty () );
    assertTrue ( cc.isEmpty () );
    assertTrue ( bcc.isEmpty () );
    assertEquals ( "INFO: No info from Unit Test Publisher\n", out.toString () );

    out.reset ();

    BuildInfo buildInfo = new BuildInfo ( build );

    PowerMockito.when ( build.getAction ( BuildInfo.class ) )
            .thenReturn ( buildInfo );

    obj.addRecipients ( context, vars, to, cc, bcc );

    assertTrue ( to.isEmpty () );
    assertTrue ( cc.isEmpty () );
    assertTrue ( bcc.isEmpty () );
    assertEquals ( "", out.toString () );

    org.jenkinsci.plugins.unittestdb.db.User u
            = new org.jenkinsci.plugins.unittestdb.db.User ();
    u.setUsername ( TESTUSER );
    org.jenkinsci.plugins.unittestdb.db.Failure f
            = new org.jenkinsci.plugins.unittestdb.db.Failure ();
    f.setUsers ( ImmutableList.of (
            new org.jenkinsci.plugins.unittestdb.db.FailureUser () ) );
    f.setUnitTest ( new UnitTest () );
    f.getUsers ().get ( 0 ).setUser ( u );

    buildInfo.addFailure ( f );

    obj.addRecipients ( context, vars, to, cc, bcc );

    assertTrue ( to.isEmpty () );
    assertTrue ( cc.isEmpty () );
    assertTrue ( bcc.isEmpty () );
    assertEquals ( "INFO: No email address for user " + TESTUSER + "\n", out
                   .toString () );

    out.reset ();

    User juser = PowerMockito.mock ( User.class );
    PowerMockito.when ( j.getUser ( TESTUSER ) ).thenReturn ( juser );
    PowerMockito.when ( juser.getDisplayName () ).thenReturn ( TESTFULLNAME );

    obj.addRecipients ( context, vars, to, cc, bcc );

    assertTrue ( to.isEmpty () );
    assertTrue ( cc.isEmpty () );
    assertTrue ( bcc.isEmpty () );
    assertEquals ( "INFO: No email address for user " + TESTUSER + "\n", out
                   .toString () );

    out.reset ();

    PowerMockito.when ( juser.getProperty (
            hudson.tasks.Mailer.UserProperty.class ) ).thenReturn (
                    new hudson.tasks.Mailer.UserProperty ( TESTEMAIL ) );

    obj.addRecipients ( context, vars, to, cc, bcc );

    assertFalse ( to.isEmpty () );
    assertTrue ( cc.isEmpty () );
    assertTrue ( bcc.isEmpty () );
    assertEquals ( "INFO: Added " + TESTEMAIL + " to list of recipients\n",
                   out.toString () );
    assertEquals ( 1, to.size () );

    assertEquals ( TESTEMAIL, to.iterator ().next ().getAddress () );
    assertEquals ( TESTFULLNAME, to.iterator ().next ().getPersonal () );
  }

}
