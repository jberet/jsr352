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

import static org.jberet.testapps.postconstruct.ItemProcessor1.setExitStatus;

import java.util.List;
import jakarta.annotation.PreDestroy;

import jakarta.annotation.PostConstruct;
import jakarta.batch.api.chunk.AbstractItemWriter;
import jakarta.batch.runtime.context.JobContext;
import jakarta.inject.Inject;

//@Named
// This class is declared with custom ref name (W1) in batch.xml
//
public class ItemWriter1 extends AbstractItemWriter {
    @Inject
    private JobContext jobContext;

    @PostConstruct
    private void postConstruct() {
        setExitStatus(jobContext, "ItemWriter1.postConstruct");
    }

    @PreDestroy
    private void preDestroy() {
        setExitStatus(jobContext, "ItemWriter1.preDestroy");
    }

    @Override
    public void writeItems(final List<Object> items) throws Exception {
        System.out.printf("Wrote items: %s%n", items);
    }
}
