package org.jenkinsci.plugins.unittestdb.project;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;

public class UnitTestDBPublisherStep extends AbstractStepImpl {

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    @DataBoundConstructor
    public UnitTestDBPublisherStep() {
        super();
    }

    @Override
    public StepDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution<UnitTestRecorder> {

        @StepContextParameter
        transient Run build;
        @StepContextParameter
        transient Launcher launcher;
        @StepContextParameter
        transient TaskListener listener;

        @Override
        protected UnitTestRecorder run() throws Exception {
            UnitTestRecorder recorder = new UnitTestRecorder(build, launcher, listener);
            recorder.record();
            return recorder;
        }
    }

    public static class DescriptorImpl extends AbstractStepDescriptorImpl {
        public DescriptorImpl() {
            super(Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "recordUnitTestsToDB";
        }

        @Override
        public String getDisplayName() {
            return "Record Unit Tests In DB";
        }
    }
}
