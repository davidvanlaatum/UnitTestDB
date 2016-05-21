package org.jenkinsci.plugins.unittestdb.reminder;

import hudson.model.Job;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.unittestdb.db.Failure;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author David van Laatum
 */
public class EMailJob {

  private static final Jenkins JENKINS = Jenkins.getInstance();
  private final Job<?, ?> project;
  protected List<EMailTest> tests = new ArrayList<> ();

  public EMailJob(Job<?, ?> project) {
    this.project = project;
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

  public Job<?, ?> getProject() {
    return project;
  }

}
