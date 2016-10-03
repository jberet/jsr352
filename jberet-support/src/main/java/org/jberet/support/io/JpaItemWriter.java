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
import java.util.List;
import java.util.Map;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemWriter;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

@Named
@Dependent
public class JpaItemWriter implements ItemWriter {

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

    @Inject
    @BatchProperty
    protected boolean entityTransaction;

    protected EntityManagerFactory emf;
    protected EntityManager em;

    @Override
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

    @Override
    public void writeItems(final List<Object> items) throws Exception {
        if(entityTransaction) {
            em.getTransaction().begin();
        }
        for (final Object e : items) {
            em.persist(e);
        }

        if (entityTransaction) {
            em.getTransaction().commit();
        }
    }

    @Override
    public void close() throws Exception {
        if (emf != null) {
            em.close();
            emf.close();
        }
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return null;
    }
}
