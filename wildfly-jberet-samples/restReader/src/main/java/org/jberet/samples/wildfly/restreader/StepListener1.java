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

package org.jberet.samples.wildfly.restreader;

import java.util.Properties;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * This step listener class sets the step exception message as the
 * job exit status for test {@code testError}. The job exit status
 * is then verified in the test client.
 */
@Named
public class StepListener1 implements javax.batch.api.listener.StepListener {
    @Inject
    private StepContext stepContext;

    @Inject
    private JobContext jobContext;

    @Override
    public void beforeStep() throws Exception {

    }

    @Override
    public void afterStep() throws Exception {
        final Exception exception = stepContext.getException();
        if (exception != null) {
            final Properties jobProperties = jobContext.getProperties();
            if (jobProperties != null) {
                final String testName = jobProperties.getProperty("testName");
                if ("testError".equals(testName)) {
                    final String message = exception.getMessage();
                    System.out.printf("For test method %s, step exception message: %s%n", testName, message);
                    //only save step exception message as exit status for test testError
                    jobContext.setExitStatus(message);
                }
            }
        }
    }

}
