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

package org.jberet.se;

import java.util.Iterator;
import java.util.Set;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.jberet.creation.AbstractArtifactFactory;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

/**
 * ArtifactFactory for Java SE runtime environment using Weld and CDI.
 */
public final class SEArtifactFactory extends AbstractArtifactFactory {
    private final BeanManager beanManager;

    public SEArtifactFactory() {
        WeldContainer weldContainer = new Weld().initialize();
        beanManager = weldContainer.getBeanManager();
    }

    @Override
    public Class<?> getArtifactClass(final String ref, final ClassLoader classLoader) {
        Bean<?> bean = getBean(ref);
        return bean == null ? null : bean.getBeanClass();
    }

    @Override
    public Object create(final String ref, Class<?> cls, final ClassLoader classLoader) throws Exception {
        Bean<?> bean = getBean(ref);
        return bean == null ? null : beanManager.getReference(bean, bean.getBeanClass(), beanManager.createCreationalContext(bean));
    }

    private Bean<?> getBean(final String ref) {
        Set<Bean<?>> beans = beanManager.getBeans(ref);
        for (Iterator<Bean<?>> it = beans.iterator(); it.hasNext(); ) {
            return it.next();
        }
        return null;
    }
}
