/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.postconstruct;

import java.util.Date;
import jakarta.annotation.PreDestroy;

import jakarta.annotation.PostConstruct;
import jakarta.batch.api.BatchProperty;
import jakarta.batch.operations.BatchRuntimeException;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.jberet.testapps.common.Batchlet0;

@Named
public class Batchlet1 extends Batchlet0 {
    @Inject @BatchProperty(name = "date")
    private Date date;

    @Override
    public String process() throws Exception {
        addToJobExitStatus("Batchlet1.process");
        return jobContext.getExitStatus();
    }

    @PostConstruct
    private void ps() {
        System.out.printf("Batchlet1 PostConstruct of %s%n", this);
        if (jobContext == null || stepContext == null || date == null) {
            throw new BatchRuntimeException("Some fields are not initialized: jobContext=" + jobContext +
            ", stepContext=" + stepContext + ", date=" + date);
        }
        addToJobExitStatus("Batchlet1.ps");
    }

    @PreDestroy
    private void pd() {
        System.out.printf("Batchlet1 PreDestroy of %s%n", this);
        addToJobExitStatus("Batchlet1.pd");
    }

}
