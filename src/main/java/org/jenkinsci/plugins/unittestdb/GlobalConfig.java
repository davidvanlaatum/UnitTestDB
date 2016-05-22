package org.jenkinsci.plugins.unittestdb;

import com.google.inject.Inject;
import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.database.Database;
import org.jenkinsci.plugins.database.jpa.PersistenceService;
import org.jenkinsci.plugins.unittestdb.db.DBObject;
import org.kohsuke.stapler.StaplerRequest;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

/**
 * @author David van Laatum
 */
@Extension
public class GlobalConfig extends GlobalConfiguration {

    private static final Jenkins JENKINS = Jenkins.getInstance();
    private static final Logger LOG
            = Logger.getLogger(GlobalConfig.class.getName());
    @Inject
    transient PersistenceService ps;
    private Database database;
    private transient EntityManagerFactory fac;

    public GlobalConfig() {
        load();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws
            FormException {
        req.bindJSON(this, json);
        save();
        try {
            if (fac != null && fac.isOpen()) {
                fac.close();
                fac = null;
            }
        } catch (NullPointerException ex) {
            fac = null;
        }
        return true;
    }

    public Database getDatabase() {
        return database;
    }

    public void setDatabase(Database database) {
        this.database = database;
        checkDB();
    }

    @Override
    public String getDisplayName() {
        return "Unit Test DB Configuration";
    }

    public EntityManagerFactory getEntityManagerFactory() throws SQLException {
        if (fac == null) {
            JENKINS.getInjector().injectMembers(this);
            requireNonNull(ps, "Persistence Service is null");
            Database db = requireNonNull(getDatabase(), "No database configured");
            DataSource ds = requireNonNull(db.getDataSource(), "No Datasource");
            List<Class> classes = DBObject.allClasses();
            LOG.log(Level.INFO, "Found DB classes {0}", classes);
            fac = requireNonNull(ps.createEntityManagerFactory(ds, classes), "No EntityManagerFactory");
        }
        return fac;
    }

    @Override
    public final synchronized void load() {
        super.load();
        try {
            checkDB();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, null, e);
        }
    }

    protected void checkDB() {
        if (database != null) {
            try (Connection conn = database.getDataSource().getConnection()) {
                liquibase.database.Database db = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(conn));
                URL changeLogFile = getClass().getResource("/unittests-schema.xml");
                requireNonNull(changeLogFile, "Failed to find changelog");
                DatabaseChangeLog changeLog = new DatabaseChangeLog(changeLogFile.toString());
                Liquibase liquibase = new Liquibase(changeLog, new ClassLoaderResourceAccessor(), db);
                liquibase.update((Contexts) null);
            } catch (LiquibaseException | SQLException ex) {
                throw new IllegalStateException("Liquibase update failed", ex);
            }
        } else {
            LOG.log(Level.INFO, "No database config skipping");
        }
    }
}
