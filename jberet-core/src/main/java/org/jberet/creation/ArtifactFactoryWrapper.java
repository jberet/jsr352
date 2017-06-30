/*
 * Copyright (c) 2012-2013 Red Hat, Inc. and/or its affiliates.
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

import javax.annotation.PostConstruct;

import org.jberet._private.BatchMessages;
import org.jberet.job.model.BatchArtifacts;
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
                obj = c.newInstance();
                ArtifactCreationContext acc = ArtifactCreationContext.getCurrentArtifactCreationContext();
                doInjection(obj, c, classLoader, acc.jobContext, acc.stepContext, acc.properties);
                invokeAnnotatedLifecycleMethod(obj, cls, PostConstruct.class);
            }
        }
        return obj;
    }

    private Class<?> getClassFromBatchXmlOrClassLoader(final String ref, final ClassLoader classLoader) {
        Class<?> cls = null;
        BatchArtifacts batchArtifacts = ArtifactCreationContext.getCurrentArtifactCreationContext().jobContext.getBatchArtifacts();
        String className = null;
        if (batchArtifacts != null) {
            className = batchArtifacts.getClassNameForRef(ref);
        }
        if (className == null) {
            className = ref;
        }
        try {
            cls = classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw BatchMessages.MESSAGES.failToCreateArtifact(e, ref);
        }
        return cls;
    }
}
