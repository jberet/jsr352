/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.postconstruct;

import jakarta.annotation.PreDestroy;

import jakarta.annotation.PostConstruct;
import jakarta.batch.api.chunk.ItemProcessor;
import jakarta.batch.runtime.context.JobContext;
import jakarta.inject.Inject;

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
