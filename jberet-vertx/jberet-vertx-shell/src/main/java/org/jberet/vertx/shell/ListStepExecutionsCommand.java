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
import javax.batch.runtime.StepExecution;

import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Option;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.ext.shell.command.CommandProcess;
import org.jberet.util.BatchUtil;

@SuppressWarnings("unused")
@Name("list-step-executions")
@Summary("List step executions belonging to a job execution")
public final class ListStepExecutionsCommand extends CommandBase {
    private long jobExecutionId;

    @Description("the job execution id whose step executions to list")
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
            process.write("Step executions in job execution ").write(String.valueOf(jobExecutionId))
                    .write(":")
                    .write(BatchUtil.NL);
            for (StepExecution stepExecution : stepExecutions) {
                process.write(String.valueOf(stepExecution.getStepExecutionId()))
                        .write("\t")
                        .write(String.valueOf(stepExecution.getBatchStatus()))
                        .write(BatchUtil.NL);
            }
            process.end();
        } catch (Exception e) {
            failed(process, e);
        }
    }
}
