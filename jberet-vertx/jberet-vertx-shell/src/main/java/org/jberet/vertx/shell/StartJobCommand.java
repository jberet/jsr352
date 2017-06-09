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
@Name("start-job")
@Summary("Start a batch job execution")
public final class StartJobCommand extends CommandBase {
    private String jobName;
    private Properties jobParameters;

    @Description("the name of the job to start")
    @Argument(index = 0, argName = "jobName")
    public void setJobName(String jobName) {
        this.jobName = jobName;
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
            final long jobExecutionId = jobOperator.start(jobName, jobParameters);
            process.write(String.format(
                    "Started job %s, and the job execution id is %s%n", jobName, jobExecutionId));
            process.end();
        } catch (Exception e) {
            failed(process, e);
        }
    }
}
