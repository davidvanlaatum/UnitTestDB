package org.jenkinsci.plugins.unittestdb.db;

import java.lang.reflect.Field;

/**
 *
 * @author David van Laatum
 */
public enum FailureUserState {

  Maybe ( "Maybe" ),
  Not_Me ( "Not Me" ),
  Was_Me ( "Was Me" ),
  Might_be_Me ( "Might be Me" );

  FailureUserState ( String value ) {
    try {
      final Field nameField = this.getClass ().getSuperclass ()
              .getDeclaredField ( "name" );
      nameField.setAccessible ( true );
      nameField.set ( this, value );
    } catch ( Exception e ) {
      throw new RuntimeException ( e );
    }
  }
}
