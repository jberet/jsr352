/*
 * Copyright (c) 2012-2013 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.testapps.common;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.batch.api.BatchProperty;
import javax.batch.api.Batchlet;
import javax.inject.Inject;

public class BatchletNoNamed extends PostConstructPreDestroyBase implements Batchlet {
    @Inject
    @BatchProperty(name = "batchlet-prop")
    protected String batchletProp;

    @Inject
    @BatchProperty(name = "reference-job-prop")
    protected String referencingJobProp;

    @Inject
    @BatchProperty(name = "reference-system-prop")
    protected String referencingSystemProp;

    @Inject
    @BatchProperty(name = "reference-job-param")
    protected String referencingJobParam;

    @Override
    public String process() throws Exception {
        System.out.printf("%nIn %s, running step %s, job batch/exit status: %s/%s, step batch/exit status: %s/%s%n, job properties: %s, step properties: %s%n%n",
                this, stepContext.getStepName(),
                jobContext.getBatchStatus(), jobContext.getExitStatus(),
                stepContext.getBatchStatus(), stepContext.getExitStatus(),
                jobContext.getProperties(), stepContext.getProperties()
        );
        return "Processed";
    }

    @Override
    public void stop() throws Exception {
    }

    //overridden in org.jberet.testapps.common.Batchlet0, so this PostConstruct should not be invoked.
    @PostConstruct
    void ps() {
        System.out.printf("BatchletNoNamed PostConstruct of %s%n", this);
        addToJobExitStatus("BatchletNoNamed.ps");
    }

    //overridden in org.jberet.testapps.common.Batchlet0, so this PostConstruct should not be invoked.
    @PreDestroy
    void pd() {
        System.out.printf("BatchletNoNamed PreDestroy of %s%n", this);
        addToJobExitStatus("BatchletNoNamed.pd");
    }

}
