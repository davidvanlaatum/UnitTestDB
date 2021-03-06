package org.jenkinsci.plugins.unittestdb.project;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tasks.junit.JUnitResultArchiver;
import org.jenkinsci.plugins.unittestdb.GlobalConfig;
import org.jenkinsci.plugins.unittestdb.TestBase;
import org.jenkinsci.plugins.unittestdb.build.BuildInfo;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.FakeChangeLogSCM;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.*;

/**
 * @author David van Laatum
 */
public class UnitTestDBPublisherTest extends TestBase {

  @Rule
  public JenkinsRule j = new JenkinsRule ();

  @Test
  public void testSingle () throws Exception {
    String UNITTESTUSER = "unittestuser";
    String PROJECTNAME = "FreeStyle";
    GlobalConfig config = j.getInstance ().getInjector ().getInstance (
            GlobalConfig.class );
    assertNotNull ( config );
    config.setDatabase ( unitTestDB () );
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
      try ( PreparedStatement stmt = conn.prepareStatement (
              "DELETE FROM nodes WHERE name = ?" ) ) {
        stmt.setString ( 1, "master" );
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
        requireNonNull ( build.getWorkspace () ).child ( "tests.xml" )
                .copyFrom ( getClass ().getClassLoader ().getResource (
                                "exampletests.xml" ) );
        return true;
      }
    };

    TestBuilder builder2 = new TestBuilder () {
      @Override
      public boolean perform ( AbstractBuild<?, ?> build,
                               Launcher launcher,
                               BuildListener listener ) throws
              InterruptedException, IOException {
        requireNonNull ( build.getWorkspace () ).child ( "tests.xml" )
                .copyFrom ( getClass ().getClassLoader ().getResource (
                                "exampletests1.xml" ) );
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
    org.jenkinsci.plugins.unittestdb.db.Job job
            = org.jenkinsci.plugins.unittestdb.db.Job.findByName ( project
                    .getName (), em, false );

    assertNotNull ( "No job", job );
    assertNotNull ( "No last build", job.getLastBuild () );

    org.jenkinsci.plugins.unittestdb.db.User user
            = org.jenkinsci.plugins.unittestdb.db.User.
            findByUsername ( UNITTESTUSER, em, false );

    assertNotNull ( "No User", user );

    build = project.scheduleBuild2 ( 0 ).get ();
    j.assertBuildStatus ( Result.UNSTABLE, build );

    BuildInfo info = build.getAction ( BuildInfo.class );
    assertNotNull ( "No build info", info );
    assertTrue ( info.hasFailures () );
    assertFalse ( info.getUsers ().isEmpty () );

    project.getBuildersList ().remove ( builder1 );
    project.getBuildersList ().add ( builder2 );

    build = project.scheduleBuild2 ( 0 ).get ();
    j.assertBuildStatus ( Result.SUCCESS, build );

    info = build.getAction ( BuildInfo.class );
    assertNotNull ( "No build info", info );
    assertTrue ( info.hasFailures () );
    assertFalse ( info.getUsers ().isEmpty () );

    build = project.scheduleBuild2 ( 0 ).get ();
    j.assertBuildStatus ( Result.SUCCESS, build );

    info = build.getAction ( BuildInfo.class );
    assertNotNull ( "No build info", info );
    assertFalse ( info.hasFailures () );
    assertTrue ( info.getUsers ().isEmpty () );
  }
/*
  @Test
  public void testMatrix () throws Exception {
    final String UNITTESTUSER = "unittestuser2";
    final String PROJECTNAME = "Matrix";
    GlobalConfig config = j.getInstance ().getInjector ().getInstance (
            GlobalConfig.class );
    assertNotNull ( config );
    config.setDatabase ( unitTestDB () );
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
      try ( PreparedStatement stmt = conn.prepareStatement (
              "DELETE FROM nodes WHERE name = ? OR name = ?" ) ) {
        stmt.setString ( 1, "master" );
        stmt.setString ( 2, "slave0" );
        stmt.execute ();
      }
    }

    j.createOnlineSlave ().setNodeName ( "slave0" );
    MatrixProject project = j.createProject(MatrixProject.class, PROJECTNAME);

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
        requireNonNull ( build.getWorkspace () ).child ( "tests.xml" )
                .copyFrom ( getClass ()
                        .getClassLoader ().getResource ( "exampletests" + build
                                .getBuildVariableResolver ().resolve ( "TEST" )
                                                                 + ".xml" ) );
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
    org.jenkinsci.plugins.unittestdb.db.Job job
            = org.jenkinsci.plugins.unittestdb.db.Job.findByName ( project
                    .getName (), em, false );

    assertNotNull ( "No Job", job );
    assertNotNull ( "No Last Build", job.getLastBuild () );

    org.jenkinsci.plugins.unittestdb.db.User user
            = org.jenkinsci.plugins.unittestdb.db.User.
            findByUsername ( UNITTESTUSER, em, false );

    assertNotNull ( "No User", user );
  }*/
}
