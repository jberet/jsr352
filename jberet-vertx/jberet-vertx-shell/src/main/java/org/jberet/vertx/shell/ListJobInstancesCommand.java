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
import javax.batch.runtime.JobInstance;

import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Option;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.ext.shell.command.CommandProcess;
import org.jberet.util.BatchUtil;

@SuppressWarnings("unused")
@Name("list-job-instances")
@Summary("List job instances")
public final class ListJobInstancesCommand extends CommandBase {
    private String jobName;
    private int start;
    private int count = 10;

    @Description("the name of the job")
    @Option(required = true, longName = "job-name", shortName = "j")
    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    @Description("the start position within all matching job instances (0-based)")
    @Option(longName = "start", shortName = "s")
    public void setStart(int start) {
        this.start = start;
    }

    @Description("the number of job instances to return, defaults to 10")
    @Option(longName = "count", shortName = "c")
    public void setCount(int count) {
        this.count = count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(final CommandProcess process) {
        try {
            final List<JobInstance> jobInstances = jobOperator.getJobInstances(jobName, start, count);
            process.write("Job instances for job: ").write(jobName).write(BatchUtil.NL);

            for (JobInstance jobInstance : jobInstances) {
                process.write(String.valueOf(jobInstance.getInstanceId())).write(BatchUtil.NL);
            }
            process.end();
        } catch (Exception e) {
            failed(process, e);
        }
    }
}
