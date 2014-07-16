package org.jenkinsci.plugins.unittestdb;

import java.lang.reflect.InvocationTargetException;
import hudson.util.Secret;
import org.jenkinsci.plugins.database.AbstractRemoteDatabase;

/**
 * @author David van Laatum
 */
public class TestBase {

  private static AbstractRemoteDatabase db = null;

  public static AbstractRemoteDatabase unitTestDB () {
    if ( db == null ) {
      try {
        db = (AbstractRemoteDatabase) Class.forName (
                System.getProperty ( "db.class",
                                     "org.jenkinsci.plugins.database.mysql.MySQLDatabase" ) )
                .getConstructor ( String.class, String.class, String.class,
                                  Secret.class, String.class )
                .newInstance ( System.getProperty ( "db.host" ),
                               System.getProperty ( "db.name" ),
                               System.getProperty ( "db.user" ),
                               Secret.fromString (
                                       System.getProperty ( "db.pass" ) ),
                               System.getProperty ( "db.properties" ) );
      } catch ( ClassNotFoundException | NoSuchMethodException |
                SecurityException | InstantiationException |
                IllegalAccessException | IllegalArgumentException |
                InvocationTargetException ex ) {
        throw new IllegalArgumentException (
                "Failed to create unit test database connection", ex );
      }
    }
    return db;
  }
}
