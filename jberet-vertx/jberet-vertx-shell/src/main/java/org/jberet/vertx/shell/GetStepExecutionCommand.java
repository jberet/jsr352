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
import org.jberet.util.BatchUtil;

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
                    process.write("execution id\t\t").write(String.valueOf(sid)).write(BatchUtil.NL)
                            .write("step name\t\t").write(se.getStepName()).write(BatchUtil.NL)
                            .write("batch status\t\t").write(se.getBatchStatus().toString()).write(BatchUtil.NL)
                            .write("exit status\t\t").write(se.getExitStatus()).write(BatchUtil.NL)
                            .write("start time\t\t").write(String.valueOf(se.getStartTime())).write(BatchUtil.NL)
                            .write("end time\t\t").write(String.valueOf(se.getEndTime())).write(BatchUtil.NL);

                    for (Metric m : se.getMetrics()) {
                        process.write(m.getType().toString())
                                .write("\t")
                                .write(String.valueOf(m.getValue()))
                                .write(BatchUtil.NL);
                    }

                    process.write("persistent data\t\t").write(String.valueOf(se.getPersistentUserData()))
                            .write(BatchUtil.NL);
                }
            }

            if (!found) {
                process.write("Didn't find step execution with step execution id ")
                        .write(String.valueOf(stepExecutionId))
                        .write(" and job execution id ")
                        .write(String.valueOf(jobExecutionId));
            }
            process.end();
        } catch (Exception e) {
            failed(process, e);
        }
    }
}
