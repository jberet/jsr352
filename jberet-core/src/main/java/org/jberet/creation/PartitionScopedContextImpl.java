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
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;

import org.jberet.cdi.PartitionScoped;
import org.jberet.runtime.context.StepContextImpl;

public class PartitionScopedContextImpl implements Context {
    static final PartitionScopedContextImpl INSTANCE = new PartitionScopedContextImpl();

    private PartitionScopedContextImpl() {
    }

    public static PartitionScopedContextImpl getInstance() {
        return INSTANCE;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return PartitionScoped.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(final Contextual<T> contextual, final CreationalContext<T> creationalContext) {
        final ConcurrentMap<Contextual<?>, JobScopedContextImpl.ScopedInstance<?>> partitionScopedBeans = getPartitionScopedBeans();
        JobScopedContextImpl.ScopedInstance<?> existing = partitionScopedBeans.get(contextual);
        if (existing == null) {
            final T instance = contextual.create(creationalContext);
            existing = partitionScopedBeans.putIfAbsent(contextual, new JobScopedContextImpl.ScopedInstance<T>(instance, creationalContext));
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
        final JobScopedContextImpl.ScopedInstance<?> existing = getPartitionScopedBeans().get(contextual);
        return existing == null ? null : (T) existing.instance;
    }

    @Override
    public boolean isActive() {
        final StepContextImpl stepContext = ArtifactCreationContext.getCurrentArtifactCreationContext().stepContext;
        return stepContext != null && stepContext.getPartitionScopedBeans() != null;
    }

    public void destroy(Contextual<?> contextual) {
        JobScopedContextImpl.ScopedInstance.destroy(getPartitionScopedBeans(), contextual);
    }

    private ConcurrentMap<Contextual<?>, JobScopedContextImpl.ScopedInstance<?>> getPartitionScopedBeans() {
        final StepContextImpl stepContext = ArtifactCreationContext.getCurrentArtifactCreationContext().stepContext;
        return stepContext.getPartitionScopedBeans();
    }
}
