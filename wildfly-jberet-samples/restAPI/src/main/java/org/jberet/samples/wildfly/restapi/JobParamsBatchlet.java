/*
 * Copyright (c) 2015-2018 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.samples.wildfly.restapi;

import javax.batch.api.AbstractBatchlet;
import javax.batch.api.BatchProperty;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class JobParamsBatchlet extends AbstractBatchlet {
    @Inject
    @BatchProperty
    private boolean fail;

    @Inject
    @BatchProperty
    private long sleepMillis;

    @Inject
    private JobContext jobContext;

    @Override
    public String process() throws Exception {
        System.out.printf("Properties in JobParamsBatchlet: fail=%s, sleepMillis=%s%n", fail, sleepMillis);
        if (sleepMillis > 0) {
            Thread.sleep(sleepMillis);
        }
        if (fail) {
            throw new RuntimeException("Configured to fail on purpose in " + jobContext.getJobName() +
                    ", batchlet " + this);
        }
        return null;
    }
}
