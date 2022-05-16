/*
 * Copyright (c) 2012-2018 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.creation;

import jakarta.annotation.PostConstruct;
import org.jberet.spi.ArtifactFactory;

public final class ArtifactFactoryWrapper extends AbstractArtifactFactory {
    private final ArtifactFactory factory;

    public ArtifactFactoryWrapper(final ArtifactFactory factory) {
        this.factory = factory;
    }

    @Override
    public void destroy(final Object instance) {
        factory.destroy(instance);
    }

    @Override
    public Class<?> getArtifactClass(final String ref, final ClassLoader classLoader) {
        Class<?> cls = factory.getArtifactClass(ref, classLoader);
        if (cls == null) {
            cls = getClassFromBatchXmlOrClassLoader(ref, classLoader);
        }
        return cls;
    }

    @Override
    public Object create(final String ref, Class<?> cls, final ClassLoader classLoader) throws Exception {
        Object obj = factory.create(ref, cls, classLoader);
        if (obj == null) {
            final Class<?> c = getClassFromBatchXmlOrClassLoader(ref, classLoader);
            if (c != null) {
                obj = c.getDeclaredConstructor().newInstance();
                ArtifactCreationContext acc = ArtifactCreationContext.getCurrentArtifactCreationContext();
                doInjection(obj, c, classLoader, acc.jobContext, acc.stepContext, acc.properties);
                invokeAnnotatedLifecycleMethod(obj, c, PostConstruct.class);
            }
        }
        return obj;
    }
}
