/*
 * Copyright (c) 2017 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.testapps.cdiscopes.commons;

import java.lang.annotation.ElementType;
import java.util.List;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

/**
 * Common super class for batch artifacts used in CDI-scope-related tests.
 */
public abstract class ScopeArtifactBase {
    @Inject
    protected StepContext stepContext;

    protected String addStepNames(final StepNameHolder typeTarget,
                                  final StepNameHolder methodTarget,
                                  final StepNameHolder fieldTarget) {
        final List<String> stepNamesTypeTarget = typeTarget.getStepNames();
        final String stepName = stepContext.getStepName();
        stepNamesTypeTarget.add(stepName + ElementType.TYPE);
        String exitStatus1 = String.join(" ", stepNamesTypeTarget);

        final List<String> stepNamesMethodTarget = methodTarget.getStepNames();
        stepNamesMethodTarget.add(stepName + ElementType.METHOD);
        String exitStatus2 = String.join(" ", stepNamesMethodTarget);

        final List<String> stepNamesFieldTarget = fieldTarget.getStepNames();
        stepNamesFieldTarget.add(stepName + ElementType.FIELD);
        String exitStatus3 = String.join(" ", stepNamesFieldTarget);

        return String.join(" ", exitStatus1, exitStatus2, exitStatus3);
    }

    public void stop() throws Exception {
    }
}
