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
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;

import io.vertx.core.cli.annotations.Argument;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Option;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.ext.shell.command.CommandProcess;

@SuppressWarnings("unused")
@Name("get-step-execution")
@Summary("Get step execution details")
public final class GetStepExecutionCommand extends CommandBase {
    private long stepExecutionId;
    private long jobExecutionId;

    @Description("the step execution id")
    @Argument(index = 0, argName = "stepExecutionId")
    public void setStepExecutionId(long i) {
        this.stepExecutionId = i;
    }

    @Description("the step execution id")
    @Option(longName = "step-execution-id", shortName = "s")
    public void setStepExecutionIdOption(long i) {
        this.stepExecutionId = i;
    }

    @Description("the job execution id")
    @Option(required = true, longName = "job-execution-id", shortName = "j")
    public void setJobExecutionId(long i) {
        this.jobExecutionId = i;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(final CommandProcess process) {
        try {
            final List<StepExecution> stepExecutions = jobOperator.getStepExecutions(jobExecutionId);
            boolean found = false;
            for (StepExecution se : stepExecutions) {
                final long sid = se.getStepExecutionId();
                if (sid == stepExecutionId) {
                    found = true;
                    process.write(format("execution id", sid))
                            .write(format("step name", se.getStepName()))
                            .write(format("batch status", se.getBatchStatus()))
                            .write(format("exit status", se.getExitStatus()))
                            .write(format("start time", se.getStartTime()))
                            .write(format("end time", se.getEndTime()));

                    for (Metric m : se.getMetrics()) {
                        process.write(format(m.getType(), m.getValue()));
                    }
                    process.write(format("persistent data", se.getPersistentUserData()));
                }
            }

            if (!found) {
                process.write(String.format(
                        "Didn't find step execution with step execution id %s and job execution id %s",
                        stepExecutionId, jobExecutionId));
            }
            process.end();
        } catch (Exception e) {
            failed(process, e);
        }
    }

    static String format(Object label, Object value) {
        return String.format("%-25s %s%n", label, value);
    }
}
