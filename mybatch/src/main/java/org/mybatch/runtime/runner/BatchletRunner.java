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
import javax.batch.operations.JobOperator;

import org.mybatch.job.Batchlet;
import org.mybatch.runtime.context.StepContextImpl;

import static org.mybatch.util.BatchLogger.LOGGER;

public class BatchletRunner extends AbstractRunner implements Callable<Object> {
    private Batchlet batchlet;
    private StepExecutionRunner stepExecutionRunner;
    private StepContextImpl stepContext;

    public BatchletRunner(Batchlet batchlet, StepExecutionRunner stepExecutionRunner) {
        this.batchlet = batchlet;
        this.stepExecutionRunner = stepExecutionRunner;
        this.stepContext = stepExecutionRunner.getStepContext();
    }

    @Override
    public Object call() {
        try {
            String ref = batchlet.getRef();
            javax.batch.api.Batchlet batchletObj =
                    (javax.batch.api.Batchlet) stepContext.getJobContext().createArtifact(ref, batchlet.getProperties(), stepContext);
            String exitStatus = batchletObj.process();
            stepContext.setExitStatus(exitStatus);
        } catch (Throwable e) {
            LOGGER.failToRunBatchlet(e, batchlet);
            stepContext.setBatchStatus(JobOperator.BatchStatus.FAILED.name());
        }
        return null;
    }

}
