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

import io.vertx.core.cli.annotations.Argument;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.ext.shell.command.CommandProcess;
import org.jberet.util.BatchUtil;

@SuppressWarnings("unused")
@Name("abandon-job-execution")
@Summary("Abandon a job execution")
public final class AbandonJobExecutionCommand extends CommandBase {
    private long jobExecutionId;

    @Description("the job execution id to abandon")
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
            jobOperator.abandon(jobExecutionId);
            process.write("Abandoned job execution: ")
                    .write(String.valueOf(jobExecutionId))
                    .write(BatchUtil.NL);
            process.end();
        } catch (Exception e) {
            failed(process, e);
        }
    }
}
