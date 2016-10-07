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

import java.util.List;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.batch.api.chunk.AbstractItemWriter;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;

import static org.jberet.testapps.postconstruct.ItemProcessor1.setExitStatus;

@Named
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
