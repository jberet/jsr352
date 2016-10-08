/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.testapps.postconstruct;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.batch.api.chunk.ItemProcessor;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;

//@Named
// This class is referenced with its fully-qualified class name in job.xml:
// chunkPostConstruct.xml
public class ItemProcessor1 implements ItemProcessor {

    @Inject
    private JobContext jobContext;

    @PostConstruct
    private void postConstruct() {
        setExitStatus(jobContext, "ItemProcessor1.postConstruct");
    }

    @Override
    public Object processItem(final Object item) throws Exception {
        return item;
    }

    @PreDestroy
    private void preDestroy() {
        setExitStatus(jobContext, "ItemProcessor1.preDestroy");
    }

    static void setExitStatus(final JobContext jobContext1, final String value) {
        System.out.printf("In %s%n", value);
        final String exitStatus = jobContext1.getExitStatus();
        final String exitStatusNew = exitStatus == null ?  value : exitStatus + " " + value;
        jobContext1.setExitStatus(exitStatusNew);
    }
}
