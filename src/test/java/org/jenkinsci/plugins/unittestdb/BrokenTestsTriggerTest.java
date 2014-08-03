package org.jenkinsci.plugins.unittestdb;

import org.jenkinsci.plugins.unittestdb.build.BuildInfo;
import org.jenkinsci.plugins.unittestdb.email.BrokenTestsTrigger;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Objects;
import hudson.console.ConsoleNote;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.TaskListener;
import hudson.util.AbstractTaskListener;
import org.jenkinsci.plugins.unittestdb.db.*;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import static org.junit.Assert.*;

/**
 *
 * @author David van Laatum
 */
public class BrokenTestsTriggerTest {

  public BrokenTestsTriggerTest () {
  }

  @Rule
  public JenkinsRule j = new JenkinsRule ();

  /**
   * Test of trigger method, of class BrokenTestsTrigger.
   */
  @Test
  public void testTrigger () throws Exception {
    BrokenTestsTrigger trigger = new BrokenTestsTrigger ( null );
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
    FreeStyleBuild build = Objects.requireNonNull ( project.getLastBuild () );

    assertFalse ( trigger.trigger ( build, listener ) );
    assertEquals ( "", out.toString () );

    out.reset ();

    BuildInfo buildInfo = new BuildInfo ( build );

    build.addAction ( buildInfo );

    assertFalse ( trigger.trigger ( build, listener ) );
    assertEquals ( "", out.toString () );

    out.reset ();

    Failure f = new Failure ();
    f.setUsers ( new ArrayList<FailureUser> () );
    f.getUsers ().add ( new FailureUser () );
    f.getUsers ().get ( 0 ).setUser ( new User ( 0, "auser" ) );
    f.setUnitTest ( new UnitTest ( 0, "abc" ) );
    f.setFirstBuild ( new Build () );
    f.setLastBuild ( new Build () );
    buildInfo.addFailure ( f );

    assertTrue ( trigger.trigger ( build, listener ) );
    assertEquals ( "", out.toString () );

  }

}
