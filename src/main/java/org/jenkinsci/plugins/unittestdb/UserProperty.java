package org.jenkinsci.plugins.unittestdb;

import java.util.logging.Level;
import java.util.logging.Logger;
import hudson.Extension;
import hudson.model.*;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.unittestdb.UserProperty;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

/**
 *
 * @author David van Laatum
 */
public class UserProperty extends hudson.model.UserProperty {

  private Boolean ignore;

  public UserProperty ( Boolean ignore ) {
    this.ignore = ignore;
  }

  @Exported
  public Boolean ignoreUser () {
    return ignore;
  }

  @Extension
  public static final class DescriptorImpl extends UserPropertyDescriptor {

    @Override
    public String getDisplayName () {
      return "Unit Test DB";
    }

    @Override
    public UserProperty newInstance ( User user ) {
      return new UserProperty ( false );
    }

    @Override
    public UserProperty newInstance ( StaplerRequest req, JSONObject formData )
            throws FormException {
      LOG.log ( Level.INFO, "Value of ignore is {0}", req.getParameter (
                "unittestdb.ignore" ) );
      return new UserProperty ( req.getParameter ( "unittestdb.ignore" ) != null );
    }
  }
  private static final Logger LOG
          = Logger.getLogger ( UserProperty.class.getName () );
}
