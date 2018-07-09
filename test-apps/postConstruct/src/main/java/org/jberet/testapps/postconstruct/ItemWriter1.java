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

import java.util.List;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.batch.api.chunk.AbstractItemWriter;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;

import static org.jberet.testapps.postconstruct.ItemProcessor1.setExitStatus;

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
