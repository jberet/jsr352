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

package org.jberet.testapps.postconstruct;

import javax.batch.runtime.BatchStatus;

import org.jberet.testapps.common.AbstractIT;
import org.junit.Assert;
import org.junit.Test;

public class PostConstructIT extends AbstractIT {
    @Test
    public void postConstructAndPreDestroy() throws Exception {
        final String expected = "PostConstructPreDestroyBase.ps JobListener1.ps JobListener1.beforeJob PostConstructPreDestroyBase.ps StepListener1.ps StepListener1.beforeStep PostConstructPreDestroyBase.ps Batchlet0.ps Batchlet1.ps Batchlet1.process Batchlet1.pd Batchlet0.pd PostConstructPreDestroyBase.pd StepListener1.afterStep StepListener1.pd PostConstructPreDestroyBase.pd PostConstructPreDestroyBase.ps Decider1.ps Decider1.decide Decider1.pd PostConstructPreDestroyBase.pd JobListener1.afterJob JobListener1.pd PostConstructPreDestroyBase.pd";
        startJobAndWait("postConstruct.xml");
        final String jobExitStatus = jobExecution.getExitStatus();
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertEquals(expected, jobExitStatus);
    }
}
