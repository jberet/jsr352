/*
 * Copyright (c) 2017 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.vertx.shell;

import java.util.List;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;

import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Option;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.ext.shell.command.CommandProcess;

import static org.jberet.vertx.shell.GetStepExecutionCommand.format;

@SuppressWarnings("unused")
@Name("list-job-executions")
@Summary("List job executions")
public final class ListJobExecutionsCommand extends CommandBase {
    private String jobName;
    private boolean running;

    @Description("the name of the job")
    @Option(required = true, longName = "job-name", shortName = "j")
    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    @Description("whether to return only running job executions")
    @Option(longName = "running", shortName = "r", flag = true)
    public void setRunning(boolean r) {
        this.running = r;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(final CommandProcess process) {
        try {
            if (running) {
                final List<Long> runningExecutions = jobOperator.getRunningExecutions(jobName);
                process.write(
                        String.format("Running job executions for job %s:%n%s%n ", jobName, runningExecutions));
            } else {
                process.write(String.format("Job executions for job %s:%n", jobName));
                final List<JobInstance> jobInstances =
                        jobOperator.getJobInstances(jobName, 0, Integer.MAX_VALUE);
                for (JobInstance jobInstance : jobInstances) {
                    final List<JobExecution> jobExecutions = jobOperator.getJobExecutions(jobInstance);
                    for (JobExecution jobExecution : jobExecutions) {
                        process.write(format(jobExecution.getExecutionId(), jobExecution.getBatchStatus()));
                    }
                }
            }
            process.end();
        } catch (Exception e) {
            failed(process, e);
        }
    }
}
