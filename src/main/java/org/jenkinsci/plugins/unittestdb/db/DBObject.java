package org.jenkinsci.plugins.unittestdb.db;

import com.google.common.collect.ImmutableList;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import java.util.ArrayList;
import java.util.List;
import jenkins.model.Jenkins;

/**
 *
 * @author David van Laatum
 */
public abstract class DBObject implements ExtensionPoint {

  /**
   * All registered {@link DBObject}s.
   */
  public static ExtensionList<DBObject> all () {
    return Jenkins.getInstance ().getExtensionList ( DBObject.class );
  }

  public static List<Class> allClasses () {
    ArrayList<Class> rt = new ArrayList<> ();

    for ( DBObject obj : all () ) {
      rt.add ( obj.getClass () );
    }

    return ImmutableList.copyOf ( rt );
  }
}
