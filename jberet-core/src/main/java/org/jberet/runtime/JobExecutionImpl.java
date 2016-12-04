/*
 * Copyright (c) 2012-2016 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.runtime;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.batch.operations.JobStartException;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.StepExecution;

import org.jberet._private.BatchLogger;
import org.jberet.creation.ArtifactCreationContext;
import org.jberet.job.model.Job;
import org.jberet.util.BatchUtil;
import org.wildfly.security.manager.WildFlySecurityManager;

public final class JobExecutionImpl extends AbstractExecution implements JobExecution, Cloneable {
    private static final long serialVersionUID = 3706885354351337764L;

    /**
     * Separator character placed in front of user when combining user and restart position.
     */
    private static final char RESTART_POSITION_USER_SEP = '"';

    private long id;

    private final JobInstanceImpl jobInstance;

    private Job substitutedJob;

    private final List<StepExecution> stepExecutions = new CopyOnWriteArrayList<StepExecution>();

    private Properties jobParameters;

    protected long createTime;
    protected long lastUpdatedTime;

    /**
     * Which job-level step, flow, decision or split to restart this job execution, if it were to be restarted.
     */
    private String restartPosition;

    /**
     * User name who started this job execution.
     */
    private String user;

    private transient CountDownLatch jobTerminationLatch = new CountDownLatch(1);
    private final AtomicBoolean stopRequested = new AtomicBoolean();
    private transient final List<JobStopNotificationListener> jobStopNotificationListeners = new ArrayList<JobStopNotificationListener>();

    public JobExecutionImpl(final JobInstanceImpl jobInstance, final Properties jobParameters) throws JobStartException {
        this.jobInstance = jobInstance;
        this.jobParameters = jobParameters;
        if (WildFlySecurityManager.isChecking()) {
            this.substitutedJob = AccessController.doPrivileged(new PrivilegedAction<Job>() {
                @Override
                public Job run() {
                    return BatchUtil.clone(jobInstance.unsubstitutedJob);
                }
            });
        } else {
            this.substitutedJob = BatchUtil.clone(jobInstance.unsubstitutedJob);
        }
        this.createTime = System.currentTimeMillis();
        setBatchStatus(BatchStatus.STARTING);
    }

    public JobExecutionImpl(final JobInstanceImpl jobInstance,
                            final long id,
                            final Properties jobParameters,
                            final Date createTime,
                            final Date startTime,
                            final Date endTime,
                            final Date lastUpdatedTime,
                            final String batchStatus,
                            final String exitStatus,
                            final String restartPositionAndUser) {
        this.jobInstance = jobInstance;
        this.jobParameters = jobParameters;
        this.id = id;

        if (createTime != null) {
            this.createTime = createTime.getTime();
        }
        if (startTime != null) {
            this.startTime = startTime.getTime();
        }
        if (endTime != null) {
            this.endTime = endTime.getTime();
        }
        if (lastUpdatedTime != null) {
            this.lastUpdatedTime = lastUpdatedTime.getTime();
        }
        this.batchStatus = BatchStatus.valueOf(batchStatus);
        this.exitStatus = exitStatus;
        splitRestartPositionAndUser(restartPositionAndUser);
    }

    public void setId(final long id) {
        this.id = id;
    }

    @Override
    public JobExecutionImpl clone() {
        JobExecutionImpl result = null;
        try {
            result = (JobExecutionImpl) super.clone();
        } catch (CloneNotSupportedException e) {
            BatchLogger.LOGGER.failToClone(e, this, getJobName(), "");
        }
        return result;
    }

    //It's possible the (fast) job is already terminated and the latch nulled when this method is called
    public void awaitTermination(final long timeout, final TimeUnit timeUnit) throws InterruptedException {
        if (jobTerminationLatch != null) {
            if (timeout <= 0) {
                jobTerminationLatch.await();
            } else {
                jobTerminationLatch.await(timeout, timeUnit);
            }
        }
    }

    public Job getSubstitutedJob() {
        return substitutedJob;
    }

    @Override
    public void setBatchStatus(final BatchStatus batchStatus) {
        super.setBatchStatus(batchStatus);
        lastUpdatedTime = System.currentTimeMillis();
    }

    @Override
    public long getExecutionId() {
        return id;
    }

    @Override
    public String getJobName() {
        return jobInstance.getJobName();
    }

    @Override
    public Date getCreateTime() {
        return new Date(createTime);
    }

    @Override
    public Date getLastUpdatedTime() {
        return new Date(lastUpdatedTime);
    }

    public JobInstanceImpl getJobInstance() {
        return jobInstance;
    }

    @Override
    public Properties getJobParameters() {
        return jobParameters;
    }

    /**
     * Adds job parameter key-value pair. Should only be used to add internal job parameters.
     *
     * @param k key of the job parameter
     * @param v value of the job parameter
     */
    public void addJobParameter(final String k, final String v) {
        if (jobParameters == null) {
            jobParameters = new Properties();
        }
        jobParameters.setProperty(k, v);
    }

    public List<StepExecution> getStepExecutions() {
        return stepExecutions;
    }

    public void addStepExecution(final StepExecution stepExecution) {
        this.stepExecutions.add(stepExecution);
        lastUpdatedTime = System.currentTimeMillis();
    }

    /**
     * Sets the restart position for subsequent restart operation.
     * When set to null, the default restarting behavior is used.
     *
     * @param restartPosition the restart position; may be null
     */
    public void setRestartPosition(final String restartPosition) {
        this.restartPosition = restartPosition;
    }

    /**
     * Gets the restart position for subsequent restart operation.
     * If null is returned, the default restarting behavior is used.
     *
     * @return the restart position; may be null
     */
    public String getRestartPosition() {
        return restartPosition;
    }

    /**
     * Gets the user who started this job execution.
     *
     * @return the user who started this job execution; may be null
     * @since 1.2.2
     * @since 1.3.0.Beta4
     */
    public String getUser() {
        return user;
    }

    /**
     * Sets the user who started this job execution.
     * @param user the user who started this job execution; may be null
     * @since 1.2.2
     * @since 1.3.0.Beta4
     */
    public void setUser(final String user) {
        this.user = user;
    }

    /**
     * Combines user and restart position into one single string value.
     * Both user and restart position may be absent. Example values can be:
     * <ul>
     *     <li>null
     *     <li>step1{@value #RESTART_POSITION_USER_SEP}user1
     *     <li>step1
     *     <li>{@value #RESTART_POSITION_USER_SEP}user1
     * </ul>
     *
     * @return the combined value of user and restart position
     * @since 1.2.2
     * @since 1.3.0.Beta4
     */
    public String combineRestartPositionAndUser() {
        if (restartPosition == null && user == null) {
            return null;
        }

        final StringBuilder sb = new StringBuilder();
        if (restartPosition != null) {
            sb.append(restartPosition);
        }
        if (user != null) {
            sb.append(RESTART_POSITION_USER_SEP).append(user);
        }
        return sb.toString();
    }

    /**
     * Splits the combined user and restart position value and assigns them
     * to the corresponding fields.
     *
     * @param restartPositionAndUser the combined user and restart position value,
     *                               typically retrieved from persistence store
     * @since 1.2.2
     * @since 1.3.0.Beta4
     */
    private void splitRestartPositionAndUser(final String restartPositionAndUser) {
        if (restartPositionAndUser != null) {
            final int i = restartPositionAndUser.indexOf(RESTART_POSITION_USER_SEP);
            if (i < 0) {
                restartPosition = restartPositionAndUser;
            } else if (i > 0) {
                restartPosition = restartPositionAndUser.substring(0, i);
                user = restartPositionAndUser.substring(i + 1);
            } else {
                user = restartPositionAndUser.substring(1);
            }
        }
    }

    public boolean isStopRequested() {
        return stopRequested.get();
    }

    public void stop() {
        stopRequested.set(true);
        final JobStopNotificationListener[] ls;
        synchronized (jobStopNotificationListeners) {
            ls = jobStopNotificationListeners.toArray(new JobStopNotificationListener[jobStopNotificationListeners.size()]);
        }
        for (final JobStopNotificationListener l : ls) {
            l.stopRequested();
        }
    }

    public void registerJobStopNotifier(final JobStopNotificationListener l) {
        synchronized (jobStopNotificationListeners) {
            jobStopNotificationListeners.add(l);
        }
    }

    public void unregisterJobStopNotifier(final JobStopNotificationListener l) {
        synchronized (jobStopNotificationListeners) {
            jobStopNotificationListeners.remove(l);
        }
    }

    public void setLastUpdatedTime(final long lastUpdatedTime) {
        this.lastUpdatedTime = lastUpdatedTime;
    }

    public void cleanUp() {
        substitutedJob = null;
        if (batchStatus == BatchStatus.COMPLETED) {
            //COMPLETED job execution cannot be restarted, so no need to keep unsubstitutedJob
            jobInstance.setUnsubstitutedJob(null);
        }

        ArtifactCreationContext.removeCurrentArtifactCreationContext();
        jobTerminationLatch.countDown();
        jobTerminationLatch = null;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final JobExecutionImpl that = (JobExecutionImpl) o;

        if (id != that.id) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}
