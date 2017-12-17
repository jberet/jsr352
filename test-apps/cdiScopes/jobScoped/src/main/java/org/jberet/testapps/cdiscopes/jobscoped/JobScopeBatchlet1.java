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

import java.lang.annotation.ElementType;
import java.util.List;
import javax.batch.api.AbstractBatchlet;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.jberet.testapps.cdiscopes.commons.FooFieldTarget;
import org.jberet.testapps.cdiscopes.commons.FooMethodTarget;

@Named
public class JobScopeBatchlet1 extends AbstractBatchlet {
    @Inject
    private Foo fooTypeTarget;

    @Inject
    @Named("jobScopedMethod")
    private FooMethodTarget fooMethodTarget;

    @Inject
    @Named("jobScopedField")
    private FooFieldTarget fooFieldTarget;

    @Inject
    private StepContext stepContext;

    @Override
    public String process() throws Exception {
        final List<String> stepNamesTypeTarget = fooTypeTarget.getStepNames();
        final String stepName = stepContext.getStepName();
        stepNamesTypeTarget.add(stepName + ElementType.TYPE);
        String exitStatus1 = String.join(" ", stepNamesTypeTarget);

        final List<String> stepNamesMethodTarget = fooMethodTarget.getStepNames();
        stepNamesMethodTarget.add(stepName + ElementType.METHOD);
        String exitStatus2 = String.join(" ", stepNamesMethodTarget);

        final List<String> stepNamesFieldTarget = fooFieldTarget.getStepNames();
        stepNamesFieldTarget.add(stepName + ElementType.FIELD);
        String exitStatus3 = String.join(" ", stepNamesFieldTarget);


        return String.join(" ", exitStatus1, exitStatus2, exitStatus3);
    }
}
