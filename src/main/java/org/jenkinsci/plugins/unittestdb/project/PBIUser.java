package org.jenkinsci.plugins.unittestdb.project;

import java.io.IOException;
import java.sql.SQLException;
import hudson.model.Action;
import hudson.model.Actionable;
import javax.persistence.EntityManager;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.unittestdb.GlobalConfig;
import org.jenkinsci.plugins.unittestdb.db.FailureUser;
import org.jenkinsci.plugins.unittestdb.db.FailureUserState;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import static java.util.Objects.requireNonNull;
import static org.jenkinsci.plugins.unittestdb.db.FailureUserState.*;

/**
 *
 * @author David van Laatum
 */
@ExportedBean
public class PBIUser extends Actionable implements Action {

  protected String username;
  protected FailureUserState state;
  protected PBIFailure failure;
  protected Integer id;

  public PBIUser ( FailureUser fu ) {
    username = fu.getUser ().getUsername ();
    state = fu.getState ();
    id = fu.getFailureUserId ();
  }

  @Override
  public String getDisplayName () {
    return null;
  }

  @Override
  public String getIconFileName () {
    return null;
  }

  @Override
  public String getSearchUrl () {
    return null;
  }

  /**
   * @return the state
   */
  @Exported
  public FailureUserState getState () {
    return state;
  }

  @Override
  public String getUrlName () {
    return null;
  }

  /**
   * @return the username
   */
  public String getUsername () {
    return username;
  }

  @Exported ( inline = true )
  public hudson.model.User getUser () {
    return requireNonNull ( Jenkins.getInstance () ).getUser ( username );
  }

  protected void doUpdateTo ( FailureUserState state ) throws SQLException {
    GlobalConfig config = requireNonNull ( Jenkins.getInstance () )
            .getInjector ().getInstance ( GlobalConfig.class );
    EntityManager em = null;
    try {
      em = config.getEntityManagerFactory ().createEntityManager ();
      em.getTransaction ().begin ();
      FailureUser fu = FailureUser.findByID ( id, em );
      fu.setState ( state );
      em.getTransaction ().commit ();
    } finally {
      if ( em != null ) {
        em.close ();
      }
    }
  }

  public void doWasme ( StaplerRequest req, StaplerResponse rsp ) throws
          SQLException, ServletException, IOException {
    doUpdateTo ( Was_Me );
    rsp.forwardToPreviousPage ( req );
  }

  public void doNotme ( StaplerRequest req, StaplerResponse rsp ) throws
          SQLException, ServletException, IOException {
    doUpdateTo ( Not_Me );
    rsp.forwardToPreviousPage ( req );
  }

  public void doMaybe ( StaplerRequest req, StaplerResponse rsp ) throws
          SQLException, ServletException, IOException {
    doUpdateTo ( Maybe );
    rsp.forwardToPreviousPage ( req );
  }

  public void doMightbeme ( StaplerRequest req, StaplerResponse rsp ) throws
          SQLException, ServletException, IOException {
    doUpdateTo ( Might_be_Me );
    rsp.forwardToPreviousPage ( req );
  }

}