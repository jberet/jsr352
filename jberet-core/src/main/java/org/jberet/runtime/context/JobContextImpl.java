/*
 * Copyright (c) 2013-2014 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.runtime.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import javax.batch.api.listener.JobListener;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.context.JobContext;

import org.jberet._private.BatchLogger;
import org.jberet._private.BatchMessages;
import org.jberet.creation.ArchiveXmlLoader;
import org.jberet.creation.ArtifactCreationContext;
import org.jberet.job.model.BatchArtifacts;
import org.jberet.job.model.Job;
import org.jberet.job.model.Listeners;
import org.jberet.job.model.PropertyResolver;
import org.jberet.job.model.RefArtifact;
import org.jberet.job.model.Step;
import org.jberet.repository.JobRepository;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.spi.ArtifactFactory;
import org.jberet.spi.BatchEnvironment;

public class JobContextImpl extends AbstractContext implements JobContext, Cloneable {
    private static final AbstractContext[] EMPTY_ABSTRACT_CONTEXT_ARRAY = new AbstractContext[0];

    JobExecutionImpl jobExecution;
    private ArtifactFactory artifactFactory;
    JobRepository jobRepository;

    private JobListener[] jobListeners;

    //to track the executed steps to detect loopback, may be accessed sub-threads (e.g., flows in split executions)
    private List<String> executedStepIds = Collections.synchronizedList(new ArrayList<String>());

    JobExecutionImpl originalToRestart;
    final BatchEnvironment batchEnvironment;
    BatchArtifacts batchArtifacts;

    public JobContextImpl(final JobExecutionImpl jobExecution,
                          final JobExecutionImpl originalToRestart,
                          final ArtifactFactory artifactFactory,
                          final JobRepository jobRepository,
                          final BatchEnvironment batchEnvironment) {
        super(jobExecution.getSubstitutedJob().getId());
        this.jobExecution = jobExecution;
        this.batchEnvironment = batchEnvironment;
        this.classLoader = batchEnvironment.getClassLoader();
        this.artifactFactory = artifactFactory;
        this.jobRepository = jobRepository;

        if (originalToRestart != null) {
            this.originalToRestart = originalToRestart;
            this.jobExecution.setRestartPosition(originalToRestart.getRestartPosition());
        }

        final PropertyResolver resolver = new PropertyResolver();
        resolver.setJobParameters(jobExecution.getJobParameters());
        resolver.resolve(jobExecution.getSubstitutedJob());
        batchArtifacts = ArchiveXmlLoader.loadBatchXml(classLoader);
        createJobListeners();
    }

    @Override
    public JobContextImpl clone() {
        JobContextImpl result = null;
        try {
            result = (JobContextImpl) super.clone();
        } catch (CloneNotSupportedException e) {
            BatchLogger.LOGGER.failToClone(e, this, getJobName(), "");
        }
        result.jobExecution = jobExecution.clone();
        return result;
    }

    public boolean isRestart() {
        return originalToRestart != null;
    }

    public List<String> getExecutedStepIds() {
        return executedStepIds;
    }

    public ArtifactFactory getArtifactFactory() {
        return artifactFactory;
    }

    public JobListener[] getJobListeners() {
        return this.jobListeners;
    }

    public Properties getJobParameters() {
        return jobExecution.getJobParameters();
    }

    @Override
    public AbstractContext[] getOuterContexts() {
        return EMPTY_ABSTRACT_CONTEXT_ARRAY;
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
        return org.jberet.job.model.Properties.toJavaUtilProperties(jobExecution.getSubstitutedJob().getProperties());
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
    public void setBatchStatus(final BatchStatus status) {
        jobExecution.setBatchStatus(status);
    }

    @Override
    public void setExitStatus(final String status) {
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

    public JobRepository getJobRepository() {
        return jobRepository;
    }

    public BatchArtifacts getBatchArtifacts() {
        return batchArtifacts;
    }

    public BatchEnvironment getBatchEnvironment() {
        return batchEnvironment;
    }

    /**
     * Creates a batch artifact by delegating to the proper ArtifactFactory and passing along data needed for artifact
     * loading and creation.
     *
     * @param ref                     ref name of the artifact
     * @param cls                     the class type of the target artifact. Either ref or cls may be specified.
     * @param props                   batch properties directly for the artifact to create (does not include any properties from upper enclosing levels)
     * @param stepContextForInjection optional StepContext, needed for step-level artifact, but not for non-step-level ones
     * @return the created artifact
     */
    public <A> A createArtifact(final String ref, final Class<?> cls, final org.jberet.job.model.Properties props, final StepContextImpl... stepContextForInjection) {
        prepareCreationContext(props, stepContextForInjection);
        final A a;
        try {
            a = (A) artifactFactory.create(ref, cls, classLoader);
        } catch (Exception e) {
            throw BatchMessages.MESSAGES.failToCreateArtifact(e, ref);
        }
        if (a == null) {
            throw BatchMessages.MESSAGES.failToCreateArtifact(null, ref);
        }
        return a;
    }

    public Class<?> getArtifactClass(final String ref) {
        prepareCreationContext(null);
        return artifactFactory.getArtifactClass(ref, classLoader);
    }

    private void prepareCreationContext(final org.jberet.job.model.Properties props, final StepContextImpl... stepContextForInjection) {
        final StepContextImpl sc = stepContextForInjection.length > 0 ? stepContextForInjection[0] : null;
        ArtifactCreationContext.resetArtifactCreationContext(this, sc, props);
    }

    public void destroyArtifact(final Object... objs) {
        for (final Object obj : objs) {
            artifactFactory.destroy(obj);
        }
    }

    public void destroyArtifact(final List<?> list) {
        for (final Object obj : list) {
            artifactFactory.destroy(obj);
        }
    }

    private void createJobListeners() {
        final Listeners listeners = jobExecution.getSubstitutedJob().getListeners();
        if (listeners != null) {
            final List<RefArtifact> listenerList = listeners.getListeners();
            final int count = listenerList.size();
            this.jobListeners = new JobListener[count];
            for (int i = 0; i < count; i++) {
                final RefArtifact listener = listenerList.get(i);
                this.jobListeners[i] = createArtifact(listener.getRef(), null, listener.getProperties());
            }
        } else {
            this.jobListeners = new JobListener[0];
        }
    }
}
