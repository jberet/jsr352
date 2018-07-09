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

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.batch.api.chunk.AbstractItemReader;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;

import static org.jberet.testapps.postconstruct.ItemProcessor1.setExitStatus;

@Named
public class ItemReader1 extends AbstractItemReader {
    private List<Integer> data = new ArrayList<Integer>();

    @Inject
    private JobContext jobContext;

    @PostConstruct
    private void postConstruct() {
        setExitStatus(jobContext, "ItemReader1.postConstruct");
        data.add(1);
    }

    @PreDestroy
    private void preDestroy() {
        setExitStatus(jobContext, "ItemReader1.preDestroy");
    }

    @Override
    public Object readItem() throws Exception {
        if (data.isEmpty()) {
            return null;
        }
        return data.remove(data.size() - 1);
    }
}
