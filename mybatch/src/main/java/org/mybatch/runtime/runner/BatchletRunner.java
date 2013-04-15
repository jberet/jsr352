/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mybatch.runtime.runner;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import javax.batch.runtime.BatchStatus;

import org.mybatch.job.Batchlet;
import org.mybatch.runtime.JobExecutionImpl;
import org.mybatch.runtime.context.StepContextImpl;
import org.mybatch.util.ConcurrencyService;

import static org.mybatch.util.BatchLogger.LOGGER;

public final class BatchletRunner extends AbstractRunner<StepContextImpl> implements Callable<Object> {
    private Batchlet batchlet;

    public BatchletRunner(StepContextImpl stepContext, CompositeExecutionRunner enclosingRunner, Batchlet batchlet) {
        super(stepContext, enclosingRunner);
        this.batchlet = batchlet;
    }

    @Override
    public Object call() {
        try {
            final javax.batch.api.Batchlet batchletObj =
                    batchContext.getJobContext().createArtifact(batchlet.getRef(), batchlet.getProperties(), batchContext);

            ConcurrencyService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        batchContext.getJobContext().getJobExecution().awaitStop(JobExecutionImpl.JOB_EXECUTION_TIMEOUT_SECONDS_DEFAULT, TimeUnit.SECONDS);
                        if (batchContext.getBatchStatus() == BatchStatus.STARTED) {
                            batchContext.setBatchStatus(BatchStatus.STOPPING);
                            batchletObj.stop();
                        }
                    } catch (Exception e) {
                        LOGGER.failToStopJob(e, batchContext.getJobContext().getJobName(), batchContext.getStepName(), batchletObj);
                    }
                }
            });

            String exitStatus = batchletObj.process();
            batchContext.setExitStatus(exitStatus);
        } catch (Throwable e) {
            LOGGER.failToRunBatchlet(e, batchlet);
            batchContext.setBatchStatus(BatchStatus.FAILED);
        }
        return null;
    }

}
