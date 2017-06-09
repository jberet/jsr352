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

import java.util.Properties;

import io.vertx.core.cli.annotations.Argument;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.ext.shell.command.CommandProcess;

@SuppressWarnings("unused")
@Name("restart-job-execution")
@Summary("Restart a batch job execution")
public final class RestartJobExecutionCommand extends CommandBase {
    private long jobExecutionId;
    private Properties jobParameters;

    @Description("the job execution id to restart")
    @Argument(index = 0, argName = "jobExecutionId")
    public void setJobExecutionId(long i) {
        this.jobExecutionId = i;
    }

    @Description("job parameters as a comma-separated list of key-value pairs")
    @Argument(index = 1, argName = "jobParameters", required = false)
    public void setJobParameters(String params) {
        jobParameters = parseJobParameters(params);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(final CommandProcess process) {
        try {
            final long restartJobExecutionId = jobOperator.restart(jobExecutionId, jobParameters);
            process.write(String.format(
                    "Restarted previous job execution %s, and the new job execution id is %s%n",
                    jobExecutionId, restartJobExecutionId));
            process.end();
        } catch (Exception e) {
            failed(process, e);
        }
    }
}
