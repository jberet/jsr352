/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.javajsl;

import java.util.Properties;
import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.api.BatchProperty;
import jakarta.batch.runtime.context.JobContext;
import jakarta.batch.runtime.context.StepContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
public class Batchlet1 extends AbstractBatchlet {
    @Inject
    private JobContext jobContext;

    @Inject
    private StepContext stepContext;

    @Inject
    @BatchProperty
    private String batchletk1;

    @Inject
    @BatchProperty
    private String batchletk2;

    @Override
    public String process() throws Exception {
        System.out.printf("In process() of %s%n", this);

        final Properties jobProperties = jobContext.getProperties();
        final Properties stepProperties = stepContext.getProperties();
        final StringBuilder exitStatus = new StringBuilder();

        for (final String k : jobProperties.stringPropertyNames()) {
            exitStatus.append(jobProperties.getProperty(k));
        }
        for (final String k : stepProperties.stringPropertyNames()) {
            exitStatus.append(stepProperties.getProperty(k));
        }
        exitStatus.append(batchletk1 == null ? "" : batchletk1);
        exitStatus.append(batchletk2 == null ? "" : batchletk2);

        return exitStatus.toString();
    }
}
