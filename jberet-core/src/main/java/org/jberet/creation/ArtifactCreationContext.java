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

package org.jberet.creation;

import org.jberet.job.model.Properties;
import org.jberet.runtime.context.JobContextImpl;
import org.jberet.runtime.context.StepContextImpl;

public final class ArtifactCreationContext {
    JobContextImpl jobContext;
    StepContextImpl stepContext;
    Properties properties;

    private static final ThreadLocal<ArtifactCreationContext> currentArtifactCreationContext = new ThreadLocal<ArtifactCreationContext>() {
        @Override
        protected ArtifactCreationContext initialValue() {
            return new ArtifactCreationContext();
        }
    };

    private ArtifactCreationContext() {
    }

    public ArtifactCreationContext(final JobContextImpl jobContext, final StepContextImpl stepContext, final Properties properties) {
        this.jobContext = jobContext;
        this.stepContext = stepContext;
        this.properties = properties;
    }

    public static ArtifactCreationContext getCurrentArtifactCreationContext() {
        return currentArtifactCreationContext.get();
    }

    public static void resetArtifactCreationContext(final JobContextImpl jobContext,
                                                    final StepContextImpl stepContext,
                                                    final Properties properties) {
        ArtifactCreationContext ac = currentArtifactCreationContext.get();
        ac.jobContext = jobContext;
        ac.stepContext = stepContext;
        ac.properties = properties;
    }

    public static void removeCurrentArtifactCreationContext() {
        currentArtifactCreationContext.remove();
    }
}
