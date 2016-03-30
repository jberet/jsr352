package org.jberet.schedule;

import java.util.Properties;
import javax.ejb.ScheduleExpression;

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

    public static JobScheduleConfigBuilder newInstance() {
        return new JobScheduleConfigBuilder();
    }

    public JobScheduleConfig build() {
        return new JobScheduleConfig(jobName, jobExecutionId, jobParameters, scheduleExpression,
                initialDelay, afterDelay, interval, persistent);
    }

    public JobScheduleConfigBuilder jobName(final String jobName) {
        this.jobName = jobName;
        return this;
    }

    public JobScheduleConfigBuilder jobExecutionId(final long jobExecutionId) {
        this.jobExecutionId = jobExecutionId;
        return this;
    }

    public JobScheduleConfigBuilder jobParameters(final Properties jobParameters) {
        this.jobParameters = jobParameters;
        return this;
    }

    public JobScheduleConfigBuilder scheduleExpression(final ScheduleExpression scheduleExpression) {
        this.scheduleExpression = scheduleExpression;
        return this;
    }

    public JobScheduleConfigBuilder initialDelay(final long initialDelay) {
        this.initialDelay = initialDelay;
        return this;
    }

    public JobScheduleConfigBuilder afterDelay(final long afterDelay) {
        this.afterDelay = afterDelay;
        return this;
    }

    public JobScheduleConfigBuilder interval(final long interval) {
        this.interval = interval;
        return this;
    }

    public JobScheduleConfigBuilder persistent(final boolean persistent) {
        this.persistent = persistent;
        return this;
    }
}