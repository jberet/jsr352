/*
 * Copyright (c) 2012-2013 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.split;

import jakarta.batch.api.BatchProperty;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jberet.testapps.common.Batchlet0;
import org.junit.jupiter.api.Assertions;

@Named
public class Batchlet1 extends Batchlet0 {
    @Inject @BatchProperty(name="reference-step-prop")
    private String referencingStepProp;

    @Override
    public String process() throws Exception {
        final String result = super.process();

        final String stepToVerify = "step11";
        if (stepToVerify.equals(stepContext.getStepName())) {
            Assertions.assertEquals("step-prop", referencingStepProp);
        }

        return result;
    }
}
