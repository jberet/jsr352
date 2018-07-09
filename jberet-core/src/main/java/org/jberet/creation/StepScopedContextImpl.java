/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.creation;

import java.lang.annotation.Annotation;
import java.util.concurrent.ConcurrentMap;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

import org.jberet.cdi.StepScoped;
import org.jberet.runtime.context.StepContextImpl;

public class StepScopedContextImpl implements Context {
    static final StepScopedContextImpl INSTANCE = new StepScopedContextImpl();

    private StepScopedContextImpl() {
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return StepScoped.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(final Contextual<T> contextual, final CreationalContext<T> creationalContext) {
        final ConcurrentMap<Contextual<?>, JobScopedContextImpl.ScopedInstance<?>> stepScopedBeans = getStepScopedBeans();
        JobScopedContextImpl.ScopedInstance<?> existing = stepScopedBeans.get(contextual);
        if (existing == null) {
            final T instance = contextual.create(creationalContext);
            existing = stepScopedBeans.putIfAbsent(contextual, new JobScopedContextImpl.ScopedInstance<T>(instance, creationalContext));
            if (existing == null) {
                return instance;
            } else {
                return (T) existing.instance;
            }
        } else {
            return (T) existing.instance;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(final Contextual<T> contextual) {
        final JobScopedContextImpl.ScopedInstance<?> existing = getStepScopedBeans().get(contextual);
        return existing == null ? null : (T) existing.instance;
    }

    @Override
    public boolean isActive() {
        return ArtifactCreationContext.getCurrentArtifactCreationContext().stepContext != null;
    }

    private ConcurrentMap<Contextual<?>, JobScopedContextImpl.ScopedInstance<?>> getStepScopedBeans() {
        final StepContextImpl stepContext = ArtifactCreationContext.getCurrentArtifactCreationContext().stepContext;
        return stepContext.getScopedBeans();
    }
}
