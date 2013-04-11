/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mybatch.runtime.context;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.batch.api.listener.JobListener;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.StepExecution;
import javax.batch.runtime.context.JobContext;

import org.mybatch.creation.ArtifactFactory;
import org.mybatch.job.Job;
import org.mybatch.job.Listener;
import org.mybatch.job.Listeners;
import org.mybatch.job.Step;
import org.mybatch.metadata.ApplicationMetaData;
import org.mybatch.repository.JobRepository;
import org.mybatch.runtime.JobExecutionImpl;
import org.mybatch.runtime.StepExecutionImpl;
import org.mybatch.util.BatchLogger;
import org.mybatch.util.BatchUtil;
import org.mybatch.util.PropertyResolver;

public class JobContextImpl extends AbstractContext implements JobContext {
    JobExecutionImpl jobExecution;

    private ApplicationMetaData applicationMetaData;
    private ArtifactFactory artifactFactory;
    JobRepository jobRepository;

    private JobListener[] jobListeners = new JobListener[0];

    private LinkedList<Step> executedSteps = new LinkedList<Step>();

    JobExecutionImpl originalToRestart;

    public JobContextImpl(JobExecutionImpl jobExecution, JobExecutionImpl originalToRestart, ArtifactFactory artifactFactory, JobRepository jobRepository) {
        super(jobExecution.getSubstitutedJob().getId());
        this.jobExecution = jobExecution;
        this.applicationMetaData = jobExecution.getJobInstance().getApplicationMetaData();
        this.classLoader = applicationMetaData.getClassLoader();
        this.artifactFactory = artifactFactory;
        this.jobRepository = jobRepository;

        if (originalToRestart != null) {
            this.originalToRestart = originalToRestart;
            this.jobExecution.setRestartPoint(originalToRestart.getRestartPoint());
        }

        PropertyResolver resolver = new PropertyResolver();
        resolver.setJobParameters(jobExecution.getJobParameters());
        resolver.resolve(jobExecution.getSubstitutedJob());
        createJobListeners();
        jobExecution.setBatchStatus(BatchStatus.STARTING);
    }

    public LinkedList<Step> getExecutedSteps() {
        return executedSteps;
    }

    public ArtifactFactory getArtifactFactory() {
        return artifactFactory;
    }

    public JobListener[] getJobListeners() {
        return this.jobListeners;
    }

    public ApplicationMetaData getApplicationMetaData() {
        return applicationMetaData;
    }

    public Properties getJobParameters() {
        return jobExecution.getJobParameters();
    }

    @Override
    public AbstractContext[] getOuterContexts() {
        return new AbstractContext[0];
    }

    @Override
    public String getJobName() {
        return jobExecution.getJobInstance().getJobName();
    }

    @Override
    public long getInstanceId() {
        return jobExecution.getJobInstance().getInstanceId();
    }

    @Override
    public long getExecutionId() {
        return jobExecution.getExecutionId();
    }

    @Override
    public Properties getProperties() {
        return BatchUtil.toJavaUtilProperties(jobExecution.getSubstitutedJob().getProperties());
    }

    @Override
    public org.mybatch.job.Properties getProperties2() {
        return jobExecution.getSubstitutedJob().getProperties();
    }

    @Override
    public BatchStatus getBatchStatus() {
        return jobExecution.getBatchStatus();
    }

    @Override
    public String getExitStatus() {
        return jobExecution.getExitStatus();
    }

    @Override
    public void setBatchStatus(BatchStatus status) {
        jobExecution.setBatchStatus(status);
    }

    @Override
    public void setExitStatus(String status) {
        jobExecution.setExitStatus(status);
    }

    @Override
    public JobContextImpl getJobContext() {
        return this;
    }

    public JobExecutionImpl getJobExecution() {
        return this.jobExecution;
    }

    public Job getJob() {
        return jobExecution.getSubstitutedJob();
    }

    /**
     * Creates a batch artifact by delegating to the proper ArtifactFactory and passing along data needed for artifact
     * loading and creation.
     *
     * @param ref                     ref name of the artifact
     * @param props                   batch properties directly for the artifact to create (does not include any properties from upper enclosing levels)
     * @param stepContextForInjection optional StepContext, needed for step-level artifact, but not for non-step-level ones
     * @return the created artifact
     */
    public <A> A createArtifact(String ref, org.mybatch.job.Properties props, StepContextImpl... stepContextForInjection) {
        Map<ArtifactFactory.DataKey, Object> artifactCreationData = new HashMap<ArtifactFactory.DataKey, Object>();
        artifactCreationData.put(ArtifactFactory.DataKey.APPLICATION_META_DATA, applicationMetaData);
        artifactCreationData.put(ArtifactFactory.DataKey.JOB_CONTEXT, this);
        if (props != null) {
            artifactCreationData.put(ArtifactFactory.DataKey.BATCH_PROPERTY, props);
        }
        if (stepContextForInjection.length > 0) {
            artifactCreationData.put(ArtifactFactory.DataKey.STEP_CONTEXT, stepContextForInjection[0]);
        }
        A a = null;
        try {
            a = (A) artifactFactory.create(ref, classLoader, artifactCreationData);
        } catch (Exception e) {
            BatchLogger.LOGGER.failToCreateArtifact(e, ref);
        }
        return a;
    }

    private void createJobListeners() {
        Listeners listeners = jobExecution.getSubstitutedJob().getListeners();
        if (listeners != null) {
            List<Listener> listenerList = listeners.getListener();
            int count = listenerList.size();
            this.jobListeners = new JobListener[count];
            for (int i = 0; i < count; i++) {
                Listener listener = listenerList.get(i);
                this.jobListeners[i] = createArtifact(listener.getRef(), listener.getProperties());
            }
        }
    }

    public void saveInactiveStepExecutions() {
        if (originalToRestart != null) {
            List<StepExecutionImpl> currentInactives = jobExecution.getInactiveStepExecutions();

            List<StepExecution> originalStepExecutions = originalToRestart.getStepExecutions();
            List<StepExecution> currentStepExecutions = jobExecution.getStepExecutions();

            for (StepExecution originalStep : originalStepExecutions) {
                boolean matched = false;
                for (StepExecution currentStep : currentStepExecutions) {
                    if (originalStep.getStepName().equals(currentStep.getStepName())) {
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    currentInactives.add((StepExecutionImpl) originalStep);
                }
            }

            for (StepExecutionImpl originalStep : originalToRestart.getInactiveStepExecutions()) {
                boolean matched = false;
                for (StepExecutionImpl currentStep : currentInactives) {
                    if (originalStep.getStepName().equals(currentStep.getStepName())) {
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    currentInactives.add(originalStep);
                }
            }
        }
    }
}
