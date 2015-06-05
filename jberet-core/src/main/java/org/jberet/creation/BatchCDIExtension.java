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

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;

public class BatchCDIExtension implements javax.enterprise.inject.spi.Extension {
    public void beforeBeanDiscovery(@Observes final BeforeBeanDiscovery beforeBeanDiscovery, final BeanManager beanManager) {
        final AnnotatedType<BatchBeanProducer> batchProducerAnnotatedType = beanManager.createAnnotatedType(BatchBeanProducer.class);
        beforeBeanDiscovery.addAnnotatedType(batchProducerAnnotatedType);
    }

    public void addContext(@Observes final AfterBeanDiscovery afterBeanDiscovery) {
        afterBeanDiscovery.addContext(JobScopedContextImpl.INSTANCE);
    }
}
