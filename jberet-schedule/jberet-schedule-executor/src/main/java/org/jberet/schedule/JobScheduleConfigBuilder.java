package org.jberet.schedule;

import java.util.Properties;
import javax.ejb.ScheduleExpression;

/**
 * Builder class for {@link JobScheduleConfig}.
 *
 * @see JobScheduleConfig
 * @since 1.3.0
 */
public final class JobScheduleConfigBuilder {
    private String jobName;
    private long jobExecutionId;
    private Properties jobParameters;
    private ScheduleExpression scheduleExpression;
    private long initialDelay;
    private long afterDelay;
    private long interval;
    private boolean persistent = true;

    private JobScheduleConfigBuilder() {
    }

    /**
     * Creates a new instance of this builder.
     * @return a new instance of {@code JobScheduleConfigBuilder}
     */
    public static JobScheduleConfigBuilder newInstance() {
        return new JobScheduleConfigBuilder();
    }

    /**
     * Builds a new instance of {@link JobScheduleConfig} from this builder instance.
     * @return a new instance of {@code JobScheduleConfig}
     */
    public JobScheduleConfig build() {
        return new JobScheduleConfig(jobName, jobExecutionId, jobParameters, scheduleExpression,
                initialDelay, afterDelay, interval, persistent);
    }

    /**
     * Sets the job XML name to this builder.
     * @param jobName job XML name
     * @return this builder
     *
     * @see JobScheduleConfig#jobName
     * @see JobScheduleConfig#getJobName()
     */
    public JobScheduleConfigBuilder jobName(final String jobName) {
        this.jobName = jobName;
        return this;
    }

    /**
     * Sets the job execution id to this builder.
     * @param jobExecutionId job execution id
     * @return this builder
     *
     * @see JobScheduleConfig#jobExecutionId
     * @see JobScheduleConfig#getJobExecutionId()
     */
    public JobScheduleConfigBuilder jobExecutionId(final long jobExecutionId) {
        this.jobExecutionId = jobExecutionId;
        return this;
    }

    /**
     * Sets the job parameters to this builder.
     * @param jobParameters job parameters
     * @return this builder
     *
     * @see JobScheduleConfig#jobParameters
     * @see JobScheduleConfig#getJobParameters()
     */
    public JobScheduleConfigBuilder jobParameters(final Properties jobParameters) {
        this.jobParameters = jobParameters;
        return this;
    }

    /**
     * Sets the schedule expression to this builder.
     * @param scheduleExpression job schedule expression
     * @return this builder
     *
     * @see JobScheduleConfig#scheduleExpression
     * @see JobScheduleConfig#getScheduleExpression()
     */
    public JobScheduleConfigBuilder scheduleExpression(final ScheduleExpression scheduleExpression) {
        this.scheduleExpression = scheduleExpression;
        return this;
    }

    /**
     * Sets the initial delay (in minutes) to this builder.
     * @param initialDelay initial delay in minutes
     * @return this builder
     *
     * @see JobScheduleConfig#initialDelay
     * @see JobScheduleConfig#getInitialDelay()
     */
    public JobScheduleConfigBuilder initialDelay(final long initialDelay) {
        this.initialDelay = initialDelay;
        return this;
    }

    /**
     * Sets the subsequent delay (in minutes) to this builder.
     * @param afterDelay subsequent delay in minutes
     * @return this builder
     *
     * @see JobScheduleConfig#afterDelay
     * @see JobScheduleConfig#getAfterDelay()
     */
    public JobScheduleConfigBuilder afterDelay(final long afterDelay) {
        this.afterDelay = afterDelay;
        return this;
    }

    /**
     * Sets the interval or period (in minutes) to this builder.
     * @param interval interval or period in minutes
     * @return this builder
     *
     * @see JobScheduleConfig#interval
     * @see JobScheduleConfig#getInterval()
     */
    public JobScheduleConfigBuilder interval(final long interval) {
        this.interval = interval;
        return this;
    }

    /**
     * Sets the persistent flat to this builder.
     * @param persistent whether the job schedule is persistent
     * @return this builder
     *
     * @see JobScheduleConfig#persistent
     * @see JobScheduleConfig#isPersistent()
     */
    public JobScheduleConfigBuilder persistent(final boolean persistent) {
        this.persistent = persistent;
        return this;
    }
}