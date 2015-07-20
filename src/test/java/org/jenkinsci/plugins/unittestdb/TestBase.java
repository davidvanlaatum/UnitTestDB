package org.jenkinsci.plugins.unittestdb;

import java.lang.reflect.InvocationTargetException;
import hudson.util.Secret;
import org.jenkinsci.plugins.database.AbstractRemoteDatabase;

import static java.util.Objects.requireNonNull;

/**
 * @author David van Laatum
 */
public class TestBase {

  private static AbstractRemoteDatabase db = null;

  public static AbstractRemoteDatabase unitTestDB () {
    if ( db == null ) {
      try {
          final String dbHost = System.getProperty ( "db.host" );
          final String dbName = System.getProperty ( "db.name" );
          final String dbUser = System.getProperty ( "db.user" );
          final Secret dbPass = Secret.fromString ( System.getProperty ( "db.pass" ) );
          final String dbProperties = System.getProperty ( "db.properties" );
          requireNonNull(dbName,"Must set db.name");
          db = (AbstractRemoteDatabase) Class.forName (
                System.getProperty ( "db.class",
                                     "org.jenkinsci.plugins.database.mysql.MySQLDatabase" ) )
                .getConstructor ( String.class, String.class, String.class,
                                  Secret.class, String.class )
              .newInstance ( dbHost, dbName, dbUser, dbPass, dbProperties );
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
