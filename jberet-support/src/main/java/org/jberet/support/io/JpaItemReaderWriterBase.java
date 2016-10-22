/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.support.io;

import java.io.Serializable;
import java.util.Map;
import javax.batch.api.BatchProperty;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * The base class for {@link JpaItemWriter} and {@link JpaItemReader}.
 *
 * @see JpaItemReader
 * @see JpaItemWriter

 * @since 1.3.0
 */
public abstract class JpaItemReaderWriterBase {

    @Inject
    protected Instance<EntityManager> entityManagerInstance;

    @Inject
    @BatchProperty
    protected String entityManagerLookupName;

    @Inject
    @BatchProperty
    protected String persistenceUnitName;

    @Inject
    @BatchProperty
    protected Map persistenceUnitProperties;

    protected EntityManagerFactory emf;
    protected EntityManager em;

    public void open(final Serializable checkpoint) throws Exception {
        InitialContext ic = null;
        try {
            if (entityManagerLookupName != null) {
                ic = new InitialContext();
                em = (EntityManager) ic.lookup(entityManagerLookupName);
            } else {
                if (entityManagerInstance != null && !entityManagerInstance.isUnsatisfied()) {
                    em = entityManagerInstance.get();
                }
                if (em == null) {
                    emf = Persistence.createEntityManagerFactory(persistenceUnitName, persistenceUnitProperties);
                    em = emf.createEntityManager();
                }
            }
        } finally {
            if (ic != null) {
                ic.close();
            }
        }
    }

    public void close() throws Exception {
        if (emf != null) {
            em.close();
            emf.close();
        }
    }

    public Serializable checkpointInfo() throws Exception {
        return null;
    }
}
