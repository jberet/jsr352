/*
 * Copyright (c) 2012-2016 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.se;

import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;

import org.jberet.creation.AbstractArtifactFactory;
import org.jboss.weld.bootstrap.api.helpers.RegistrySingletonProvider;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

/**
 * ArtifactFactory for Java SE runtime environment using Weld and CDI.
 */
public final class SEArtifactFactory extends AbstractArtifactFactory {
    private final BeanManager beanManager;

    public SEArtifactFactory() {
        WeldContainer weldContainer;
        synchronized (SEArtifactFactory.class) {
            weldContainer = WeldContainer.instance(RegistrySingletonProvider.STATIC_INSTANCE);
            if (weldContainer == null) {
                weldContainer = new Weld(RegistrySingletonProvider.STATIC_INSTANCE).initialize();
            }
        }
        beanManager = weldContainer.getBeanManager();
    }

    @Override
    public Class<?> getArtifactClass(final String ref, final ClassLoader classLoader) {
        final Bean<?> bean = getBean(ref);
        return bean == null ? null : bean.getBeanClass();
    }

    @Override
    public Object create(final String ref, final Class<?> cls, final ClassLoader classLoader) throws Exception {
        final Bean<?> bean = getBean(ref);
        return bean == null ? null : beanManager.getReference(bean, bean.getBeanClass(), beanManager.createCreationalContext(bean));
    }

    private Bean<?> getBean(final String ref) {
        return beanManager.resolve(beanManager.getBeans(ref));
    }
}
