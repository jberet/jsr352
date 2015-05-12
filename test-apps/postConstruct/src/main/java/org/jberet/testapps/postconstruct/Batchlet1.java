/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
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

import java.util.Date;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.batch.api.BatchProperty;
import javax.batch.operations.BatchRuntimeException;
import javax.inject.Inject;
import javax.inject.Named;

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
