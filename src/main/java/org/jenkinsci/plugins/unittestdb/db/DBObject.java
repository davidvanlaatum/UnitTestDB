package org.jenkinsci.plugins.unittestdb.db;

import java.util.ArrayList;
import java.util.List;
import com.google.common.collect.ImmutableList;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import jenkins.model.Jenkins;

/**
 *
 * @author David van Laatum
 */
public abstract class DBObject implements ExtensionPoint {

  private static final Jenkins JENKINS = Jenkins.getInstance ();

  public DBObject () {

  }

  /**
   * All registered {@link DBObject}s.
   *
   * @return
   */
  public static ExtensionList<DBObject> all () {
    return JENKINS.getExtensionList ( DBObject.class );
  }

  public static List<Class> allClasses () {
    List<Class> rt = new ArrayList<> ();

    for ( DBObject obj : all () ) {
      rt.add ( obj.getClass () );
    }

    return ImmutableList.copyOf ( rt );
  }
}
