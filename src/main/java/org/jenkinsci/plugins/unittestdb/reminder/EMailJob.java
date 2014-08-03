package org.jenkinsci.plugins.unittestdb.reminder;

import java.util.ArrayList;
import java.util.List;
import hudson.model.AbstractProject;
import org.jenkinsci.plugins.unittestdb.db.Failure;
import org.jenkinsci.plugins.unittestdb.db.Job;

/**
 *
 * @author David van Laatum
 */
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
