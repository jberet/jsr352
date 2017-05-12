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

import javax.batch.runtime.JobExecution;

import io.vertx.core.cli.annotations.Argument;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.ext.shell.command.CommandProcess;
import org.jberet.util.BatchUtil;

@SuppressWarnings("unused")
@Name("get-job-execution")
@Summary("Get job execution details")
public final class GetJobExecutionCommand extends CommandBase {
    private long jobExecutionId;

    @Description("the job execution id")
    @Argument(index = 0, argName = "jobExecutionId")
    public void setJobExecutionId(long i) {
        this.jobExecutionId = i;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(final CommandProcess process) {
        try {
            final JobExecution je = jobOperator.getJobExecution(jobExecutionId);
            process.write("execution id\t\t").write(String.valueOf(jobExecutionId)).write(BatchUtil.NL)
                    .write("job name\t\t").write(je.getJobName()).write(BatchUtil.NL)
                    .write("batch status\t\t").write(je.getBatchStatus().toString()).write(BatchUtil.NL)
                    .write("exit status\t\t").write(je.getExitStatus()).write(BatchUtil.NL)
                    .write("create time\t\t").write(String.valueOf(je.getCreateTime())).write(BatchUtil.NL)
                    .write("start time\t\t").write(String.valueOf(je.getStartTime())).write(BatchUtil.NL)
                    .write("update time\t\t").write(String.valueOf(je.getLastUpdatedTime())).write(BatchUtil.NL)
                    .write("end time\t\t").write(String.valueOf(je.getEndTime())).write(BatchUtil.NL)
                    .write("job params\t\t").write(String.valueOf(je.getJobParameters()))
                    .write(BatchUtil.NL);
            process.end();
        } catch (Exception e) {
            failed(process, e);
        }
    }
}
