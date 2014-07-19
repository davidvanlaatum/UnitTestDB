package org.jenkinsci.plugins.unittestdb;

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
import org.jenkinsci.plugins.unittestdb.db.Failure;
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

    BuildInfo buildInfo = new BuildInfo ();
    buildInfo.users = new ArrayList<> ();
    buildInfo.failures = new ArrayList<> ();

    build.addAction ( buildInfo );

    assertFalse ( trigger.trigger ( build, listener ) );
    assertEquals ( "", out.toString () );

    out.reset ();

    buildInfo.failures.add ( new Failure () );

    assertTrue ( trigger.trigger ( build, listener ) );
    assertEquals ( "", out.toString () );

  }

}
