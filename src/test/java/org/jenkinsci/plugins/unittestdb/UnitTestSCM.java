package org.jenkinsci.plugins.unittestdb;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.User;
import hudson.scm.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import org.xml.sax.SAXException;

/**
 * @author David van Laatum
 */
public class UnitTestSCM extends SCM {

  @Override
  public ChangeLogParser createChangeLogParser () {
    return new ChangeLogParser () {
      @Override
      public ChangeLogSet<? extends ChangeLogSet.Entry> parse ( Run build,
                                                                RepositoryBrowser<?> browser,
                                                                File changelogFile )
              throws IOException, SAXException {
        final List<ChangeLogSet.Entry> changeset = new ArrayList<> ();
        changeset.add ( new ChangeLogSet.Entry () {

          @Override
          public Collection<String> getAffectedPaths () {
            return Collections.singletonList ( "/" );
          }

          @Override
          public User getAuthor () {
            return User.get ( "david" );
          }

          @Override
          public String getMsg () {
            return "An Example Change Set";
          }
        } );

        ChangeLogSet<ChangeLogSet.Entry> rt
                = new ChangeLogSet<ChangeLogSet.Entry> ( build, browser ) {

                  @Override
                  public boolean isEmptySet () {
                    return false;
                  }

                  @Override
                  public Iterator<ChangeLogSet.Entry> iterator () {
                    return changeset.iterator ();
                  }
                };
        return rt;
      }
    };
  }

  @Override
  public void checkout (
          Run<?, ?> build, Launcher launcher, FilePath workspace,
          TaskListener listener, File changelogFile, SCMRevisionState baseline )
          throws IOException, InterruptedException {
    changelogFile.createNewFile ();
  }

  public static final UnitTestSCM.DescriptorImpl DESCRIPTOR
          = new UnitTestSCM.DescriptorImpl ();

  @Override
  public SCMDescriptor<UnitTestSCM> getDescriptor () {
    return UnitTestSCM.DESCRIPTOR;
  }

  public static class DescriptorImpl extends SCMDescriptor<UnitTestSCM> {

    protected DescriptorImpl () {
      super ( UnitTestSCM.class, null );
      load ();
    }

    @Override
    public String getDisplayName () {
      return "UnitTestSCM";
    }
  }

}
