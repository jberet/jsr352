/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */
 
package org.jberet.testapps.cdiscopes.jobscoped;

import java.util.List;
import javax.batch.api.AbstractBatchlet;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class CdiScopeBatchlet1 extends AbstractBatchlet {
    @Inject
    private Foo foo;

    @Inject
    private StepContext stepContext;

    @Override
    public String process() throws Exception {
        final List<String> stepNames = foo.getStepNames();
        stepNames.add(stepContext.getStepName());
        System.out.printf("In %s, foo.stepNames: %s%n", this, stepNames);
        return stepNames.toString();
    }
}
