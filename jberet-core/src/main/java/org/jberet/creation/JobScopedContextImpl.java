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
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

import org.jberet.cdi.JobScoped;
import org.jberet.runtime.context.JobContextImpl;

public class JobScopedContextImpl implements Context {
    static final JobScopedContextImpl INSTANCE = new JobScopedContextImpl();

    private JobScopedContextImpl() {
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return JobScoped.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(final Contextual<T> contextual, final CreationalContext<T> creationalContext) {
        final ConcurrentMap<Contextual<?>, ScopedInstance<?>> jobScopedBeans = getJobScopedBeans();
        ScopedInstance<?> existing = jobScopedBeans.get(contextual);
        if (existing == null) {
            final T instance = contextual.create(creationalContext);
            existing = jobScopedBeans.putIfAbsent(contextual, new ScopedInstance<T>(instance, creationalContext));
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
        final ScopedInstance<?> existing = getJobScopedBeans().get(contextual);
        return existing == null ? null : (T) existing.instance;
    }

    @Override
    public boolean isActive() {
        return ArtifactCreationContext.getCurrentArtifactCreationContext().jobContext != null;
    }

    private ConcurrentMap<Contextual<?>, ScopedInstance<?>> getJobScopedBeans() {
        final JobContextImpl jobContext = ArtifactCreationContext.getCurrentArtifactCreationContext().jobContext;
        return jobContext.getScopedBeans();
    }

    public static final class ScopedInstance<T> {
        final T instance;
        final CreationalContext<T> creationalContext;

        public ScopedInstance(final T instance, final CreationalContext<T> creationalContext) {
            this.instance = instance;
            this.creationalContext = creationalContext;
        }

        @SuppressWarnings("unchecked")
        public static void destroy(final ConcurrentMap<Contextual<?>, JobScopedContextImpl.ScopedInstance<?>> scopedBeans) {
            if (scopedBeans.size() > 0) {
                for (final Map.Entry<Contextual<?>, JobScopedContextImpl.ScopedInstance<?>> e : scopedBeans.entrySet()) {
                    final Contextual<?> contextual = e.getKey();
                    final ScopedInstance<?> value = e.getValue();
                    ((Contextual) contextual).destroy(value.instance, value.creationalContext);
                }
                scopedBeans.clear();
            }
        }
    }
}
