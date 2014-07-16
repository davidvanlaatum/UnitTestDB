package org.jenkinsci.plugins.unittestdb;

import java.sql.SQLException;
import com.google.inject.Inject;
import hudson.Extension;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.database.Database;
import org.jenkinsci.plugins.database.jpa.PersistenceService;
import org.jenkinsci.plugins.unittestdb.DB.DBObject;
import org.kohsuke.stapler.StaplerRequest;
import static java.util.Objects.requireNonNull;

/**
 * @author David van Laatum
 */
@Extension
public class GlobalConfig extends GlobalConfiguration {

  private Database database;
  private EntityManagerFactory fac;
  @Inject
  PersistenceService ps;

  public GlobalConfig () {
    load ();
  }

  @Override
  public boolean configure ( StaplerRequest req, JSONObject json ) throws
          FormException {
    req.bindJSON ( this, json );
    save ();
    if ( fac != null ) {
      fac.close ();
      fac = null;
    }
    return true;
  }

  public Database getDatabase () {
    return database;
  }

  public void setDatabase ( Database database ) {
    this.database = database;
  }

  @Override
  public String getDisplayName () {
    return "Unit Test DB Configuration";
  }

  public EntityManagerFactory getEntityManagerFactory () throws SQLException {
    if ( fac == null ) {
      Jenkins.getInstance ().getInjector ().injectMembers ( this );
      requireNonNull ( ps, "Persistence Service is null" );
      Database db = requireNonNull ( getDatabase (),
                                     "No database configured" );
      DataSource ds = requireNonNull ( db.getDataSource (), "No Datasource" );
      fac = requireNonNull ( ps
              .createEntityManagerFactory ( ds, DBObject.allClasses () ),
                             "No EntityManagerFactory" );
    }
    return fac;
  }
}
