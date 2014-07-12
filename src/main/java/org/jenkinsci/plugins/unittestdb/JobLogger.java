package org.jenkinsci.plugins.unittestdb;

import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 *
 * @author David van Laatum
 */
public class JobLogger extends Handler {

  protected PrintStream stream;

  JobLogger ( PrintStream logger ) {
    stream = logger;
  }

  @Override
  public void close () throws SecurityException {
  }

  @Override
  public void flush () {
  }

  @Override
  public void publish ( LogRecord record ) {
    StringBuilder msg = new StringBuilder ();

    Object[] parameters = record.getParameters ();
    if ( parameters != null ) {
      msg.append ( MessageFormat.format ( record.getMessage (), parameters ) );
    } else {
      msg.append ( record.getMessage () );
    }
    Throwable e = record.getThrown ();
    stream.format ( "%1$s: %2$s\n", record.getLevel (), msg.toString () );
    if ( e != null ) {
      e.printStackTrace ( stream );
    }
  }

}
