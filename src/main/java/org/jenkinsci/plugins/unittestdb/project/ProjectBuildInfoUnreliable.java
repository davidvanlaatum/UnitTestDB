package org.jenkinsci.plugins.unittestdb.project;

import org.jenkinsci.plugins.unittestdb.db.UnitTest;

/**
 *
 * @author David van Laatum
 */
public class ProjectBuildInfoUnreliable {

  String id;
  String name;
  Integer runs;
  Integer stateChanges;

  ProjectBuildInfoUnreliable ( UnitTest test ) {
    id = test.getId ();
    name = test.getName ();
    runs = test.getRuns ();
    stateChanges = test.getStatechanges ();
  }

  public String getId () {
    return id;
  }

  public String getName () {
    return name;
  }

  public Integer getRuns () {
    return runs;
  }

  public Integer getStateChanges () {
    return stateChanges;
  }

  public Long getRate () {
    return Math.round ( ( stateChanges / (double) runs ) * 100 );
  }

}
