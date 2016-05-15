package org.jenkinsci.plugins.unittestdb.project;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.unittestdb.build.BuildInfo;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class UnitTestBreakersStep extends AbstractStepImpl {

    @Extension
    public static final UnitTestBreakersStep.DescriptorImpl DESCRIPTOR = new UnitTestBreakersStep.DescriptorImpl();
    private static final Jenkins JENKINS = Jenkins.getInstance();

    @DataBoundConstructor
    public UnitTestBreakersStep() {
        super();
    }

    @Override
    public StepDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution<String> {

        @StepContextParameter
        transient Run build;
        @StepContextParameter
        transient TaskListener listener;

        @Override
        protected String run() throws Exception {
            BuildInfo buildInfo = build.getAction(BuildInfo.class);
            StringBuilder rt = new StringBuilder();
            if (buildInfo != null) {
                Set<InternetAddress> emails = new HashSet<>();
                for (String username : buildInfo.getUsers()) {
                    addUserTo(username, emails);
                }
                Iterator<InternetAddress> iterator = emails.iterator();
                while (iterator.hasNext()) {
                    InternetAddress address = iterator.next();
                    rt.append(address.toString());
                    if (iterator.hasNext()) {
                        rt.append(" ");
                    }
                }
            } else {
                throw new NullPointerException("No build info");
            }
            return rt.toString();
        }

        private void addUserTo(String u, Set<InternetAddress> to) {
            hudson.model.User user = JENKINS.getUser(u);
            if (user != null) {
                hudson.tasks.Mailer.UserProperty email = user.getProperty(hudson.tasks.Mailer.UserProperty.class);
                if (email != null) {
                    String address = email.getAddress();
                    if (address != null && !address.isEmpty()) {
                        try {
                            listener.getLogger().println(address);
                            InternetAddress a = new InternetAddress(address);
                            to.add(a);
                        } catch (AddressException ex) {
                            listener.error("%1$s", ex);
                        }
                    }
                }
            }
        }
    }

    public static class DescriptorImpl extends AbstractStepDescriptorImpl {
        public DescriptorImpl() {
            super(UnitTestBreakersStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "unitTestBreakersEMails";
        }

        @Override
        public String getDisplayName() {
            return "Return a list of EMail addresses for commiters breaking the build";
        }
    }
}
