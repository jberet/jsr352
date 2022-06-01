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

    public static JobScopedContextImpl getInstance() {
        return INSTANCE;
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

    public void destroy(Contextual<?> contextual) {
        JobScopedContextImpl.ScopedInstance.destroy(getJobScopedBeans(), contextual);
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

        public static void destroy(final ConcurrentMap<Contextual<?>, JobScopedContextImpl.ScopedInstance<?>> scopedBeans) {
            destroy(scopedBeans, null);
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        public static void destroy(final ConcurrentMap<Contextual<?>, JobScopedContextImpl.ScopedInstance<?>> scopedBeans,
                                   final Contextual<?> contextual) {
            if (contextual == null) {
                if (scopedBeans.size() > 0) {
                    for (final Map.Entry<Contextual<?>, JobScopedContextImpl.ScopedInstance<?>> e : scopedBeans.entrySet()) {
                        final Contextual<?> key = e.getKey();
                        final ScopedInstance<?> value = e.getValue();
                        ((Contextual) key).destroy(value.instance, value.creationalContext);
                    }
                    scopedBeans.clear();
                }
            } else {
                final ScopedInstance<?> value = scopedBeans.remove(contextual);
                if (value != null) {
                    ((Contextual) contextual).destroy(value.instance, value.creationalContext);
                }
            }
        }
    }
}
