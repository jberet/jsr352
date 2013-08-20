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

package org.jberet.testapps.split;

import javax.batch.api.BatchProperty;
import javax.inject.Inject;
import javax.inject.Named;

import org.jberet.testapps.common.Batchlet0;
import org.junit.Assert;

@Named
public class Batchlet1 extends Batchlet0 {
    @Inject @BatchProperty(name="reference-step-prop")
    private String referencingStepProp;

    @Override
    public String process() throws Exception {
        final String result = super.process();

        final String stepToVerify = "step11";
        if (stepToVerify.equals(stepContext.getStepName())) {
            Assert.assertEquals("step-prop", referencingStepProp);
        }

        return result;
    }
}
