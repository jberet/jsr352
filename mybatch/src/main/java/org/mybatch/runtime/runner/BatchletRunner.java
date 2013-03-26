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
import javax.batch.runtime.BatchStatus;

import org.mybatch.job.Batchlet;
import org.mybatch.runtime.context.StepContextImpl;

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
            javax.batch.api.Batchlet batchletObj =
                    (javax.batch.api.Batchlet) batchContext.getJobContext().createArtifact(batchlet.getRef(), batchlet.getProperties(), batchContext);
            String exitStatus = batchletObj.process();
            batchContext.setExitStatus(exitStatus);
        } catch (Throwable e) {
            LOGGER.failToRunBatchlet(e, batchlet);
            batchContext.setBatchStatus(BatchStatus.FAILED);
        }
        return null;
    }

}
