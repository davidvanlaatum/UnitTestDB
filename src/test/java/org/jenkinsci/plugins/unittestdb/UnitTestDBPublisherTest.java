package org.jenkinsci.plugins.unittestdb;

import com.google.common.collect.Lists;
import hudson.Launcher;
import hudson.matrix.*;
import hudson.model.*;
import hudson.slaves.DumbSlave;
import hudson.tasks.junit.JUnitResultArchiver;
import hudson.util.Secret;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.persistence.EntityManager;
import org.jenkinsci.plugins.database.mysql.MySQLDatabase;
import org.junit.*;
import org.jvnet.hudson.test.FakeChangeLogSCM;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;

import static org.junit.Assert.assertNotNull;

/**
 *
 * @author David van Laatum
 */
public class UnitTestDBPublisherTest {

  @Rule
  public JenkinsRule j = new JenkinsRule ();

  @Test
  public void testSingle () throws Exception {
    String UNITTESTUSER = "unittestuser";
    String PROJECTNAME = "FreeStyle";
    GlobalConfig config = j.getInstance ().getInjector ().getInstance (
            GlobalConfig.class );
    assertNotNull ( config );
    config.setDatabase ( new MySQLDatabase ( System.getProperty ( "db.host" ),
                                             System.getProperty ( "db.name" ),
                                             System.getProperty ( "db.user" ),
                                             Secret.fromString ( System
                                                     .getProperty ( "db.pass" ) ),
                                             null ) );

    try ( Connection conn = config.getDatabase ().getDataSource ()
            .getConnection () ) {
      try ( PreparedStatement stmt = conn.prepareStatement (
              "DELETE FROM jobs WHERE name = ?" ) ) {
        stmt.setString ( 1, PROJECTNAME );
        stmt.execute ();
      }
      try ( PreparedStatement stmt = conn.prepareStatement (
              "DELETE FROM users WHERE username = ?" ) ) {
        stmt.setString ( 1, UNITTESTUSER );
        stmt.execute ();
      }
    }

    FreeStyleProject project = j.createFreeStyleProject ( PROJECTNAME );

    TestBuilder builder1 = new TestBuilder () {
      @Override
      public boolean perform ( AbstractBuild<?, ?> build,
                               Launcher launcher,
                               BuildListener listener ) throws
              InterruptedException, IOException {
        build.getWorkspace ().child ( "tests.xml" ).copyFrom ( getClass ()
                .getClassLoader ().getResource ( "exampletests.xml" ) );
        return true;
      }
    };

    TestBuilder builder2 = new TestBuilder () {
      @Override
      public boolean perform ( AbstractBuild<?, ?> build,
                               Launcher launcher,
                               BuildListener listener ) throws
              InterruptedException, IOException {
        build.getWorkspace ().child ( "tests.xml" ).copyFrom ( getClass ()
                .getClassLoader ().getResource ( "exampletests1.xml" ) );
        return true;
      }
    };

    FakeChangeLogSCM fakeSCM = new FakeChangeLogSCM ();

    fakeSCM.addChange ().withAuthor ( UNITTESTUSER )
            .withMsg ( "A Test Commit" );

    project.setScm ( fakeSCM );
    JUnitResultArchiver jUnitResultArchiver
            = new JUnitResultArchiver ( "tests.xml", true, null );
    project.getPublishersList ().add ( jUnitResultArchiver );
    project.getPublishersList ().add ( new UnitTestDBPublisher () );
    project.getBuildersList ().add ( builder1 );

    FreeStyleBuild build = project.scheduleBuild2 ( 0 ).get ();
    j.assertBuildStatus ( Result.UNSTABLE, build );

    EntityManager em = config.getEntityManagerFactory ().createEntityManager ();
    org.jenkinsci.plugins.unittestdb.DB.Job job
            = org.jenkinsci.plugins.unittestdb.DB.Job.findByName ( project
                    .getName (), em, false );

    assertNotNull ( "No job", job );
    assertNotNull ( "No last build", job.getLastBuild () );

    org.jenkinsci.plugins.unittestdb.DB.User user
            = org.jenkinsci.plugins.unittestdb.DB.User.
            findByUsername ( UNITTESTUSER, em, false );

    assertNotNull ( "No User", user );

    build = project.scheduleBuild2 ( 0 ).get ();
    j.assertBuildStatus ( Result.UNSTABLE, build );

    project.getBuildersList ().remove ( builder1 );
    project.getBuildersList ().add ( builder2 );

    build = project.scheduleBuild2 ( 0 ).get ();
    j.assertBuildStatus ( Result.SUCCESS, build );
  }

  @Test
  public void testMatrix () throws Exception {
    final String UNITTESTUSER = "unittestuser2";
    final String PROJECTNAME = "Matrix";
    GlobalConfig config = j.getInstance ().getInjector ().getInstance (
            GlobalConfig.class );
    assertNotNull ( config );
    config.setDatabase ( new MySQLDatabase ( System.getProperty ( "db.host" ),
                                             System.getProperty ( "db.name" ),
                                             System.getProperty ( "db.user" ),
                                             Secret.fromString ( System
                                                     .getProperty ( "db.pass" ) ),
                                             null ) );
    try ( Connection conn = config.getDatabase ().getDataSource ()
            .getConnection () ) {
      try ( PreparedStatement stmt = conn.prepareStatement (
              "DELETE FROM jobs WHERE name = ?" ) ) {
        stmt.setString ( 1, PROJECTNAME );
        stmt.execute ();
      }
      try ( PreparedStatement stmt = conn.prepareStatement (
              "DELETE FROM users WHERE username = ?" ) ) {
        stmt.setString ( 1, UNITTESTUSER );
        stmt.execute ();
      }
    }

    j.createOnlineSlave ();
    MatrixProject project = j.createMatrixProject ( PROJECTNAME );

    final FakeChangeLogSCM fakeSCM = new FakeChangeLogSCM ();

    fakeSCM.addChange ().withAuthor ( UNITTESTUSER )
            .withMsg ( "A Test Commit" );

    project.setScm ( fakeSCM );

    TestBuilder builder = new TestBuilder () {
      @Override
      public boolean perform ( AbstractBuild<?, ?> build,
                               Launcher launcher,
                               BuildListener listener ) throws
              InterruptedException, IOException {
        // for some reason adding a change log entry here makes it work with matrix builds
        fakeSCM.addChange ().withAuthor ( UNITTESTUSER )
                .withMsg ( "A Test Commit" );
        build.getWorkspace ().child ( "tests.xml" ).copyFrom ( getClass ()
                .getClassLoader ().getResource ( "exampletests" + build
                        .getBuildVariableResolver ().resolve ( "TEST" ) + ".xml" ) );
        return true;
      }
    };

    project.setAxes ( new AxisList ( new Axis ( "TEST", Lists
                                                .asList ( "1",
                                                          new String[]{ "2", "3",
                                                                        "4", "5",
                                                                        "6", "7",
                                                                        "8" } ) ) ) );

    project.getBuildersList ().add ( builder );
    JUnitResultArchiver jUnitResultArchiver
            = new JUnitResultArchiver ( "tests.xml", true, null );
    project.getPublishersList ().add ( jUnitResultArchiver );
    project.getPublishersList ().add ( new UnitTestDBPublisher () );

    MatrixBuild build = project.scheduleBuild2 ( 0 ).get ();
    j.assertBuildStatus ( Result.UNSTABLE, build );

    EntityManager em = config.getEntityManagerFactory ().createEntityManager ();
    org.jenkinsci.plugins.unittestdb.DB.Job job
            = org.jenkinsci.plugins.unittestdb.DB.Job.findByName ( project
                    .getName (), em, false );

    assertNotNull ( "No Job", job );
    assertNotNull ( "No Last Build", job.getLastBuild () );

    org.jenkinsci.plugins.unittestdb.DB.User user
            = org.jenkinsci.plugins.unittestdb.DB.User.
            findByUsername ( UNITTESTUSER, em, false );

    assertNotNull ( "No User", user );
  }
}
