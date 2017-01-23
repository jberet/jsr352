/*
 * Copyright (c) 2016-2017 Red Hat, Inc. and/or its affiliates.
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
    /**
     * {@code javax.enterprise.inject.Instance} that holds optional injection
     * of {@code EntityManager}. If {@link #entityManagerLookupName} is not
     * specified, this field will be checked to obtain {@code EntityManager}.
     */
    @Inject
    protected Instance<EntityManager> entityManagerInstance;

    /**
     * JNDI lookup name of {@code EntityManager}. Optional property, and defaults
     * to null.  If specified, it will be used to perform a JNDI lookup of the
     * {@code EntityManager}.
     */
    @Inject
    @BatchProperty
    protected String entityManagerLookupName;

    /**
     * Persistence unit name. Optional property and defaults to null.
     * If neither {@link #entityManagerLookupName} nor {@link #entityManagerInstance}
     * is initialized with injected value, this persistence unit name will be used
     * to create {@EntityManagerFactory} and {@code EntityManager}.
     */
    @Inject
    @BatchProperty
    protected String persistenceUnitName;

    /**
     * Persistence unit properties, as a list of key-value pairs separated by comma (,).
     * Optional property and defaults to null.
     */
    @Inject
    @BatchProperty
    protected Map persistenceUnitProperties;

    protected EntityManagerFactory emf;
    protected EntityManager em;

    /**
     * The open method prepares the writer to write items.
     *
     * The input parameter represents the last checkpoint
     * for this writer in a given job instance. The
     * checkpoint data is defined by this writer and is
     * provided by the checkpointInfo method. The checkpoint
     * data provides the writer whatever information it needs
     * to resume writing items upon restart. A checkpoint value
     * of null is passed upon initial start.
     *
     * @param checkpoint specifies the last checkpoint
     * @throws Exception is thrown for any errors.
     */
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

    /**
     * The close method marks the end of use of the
     * ItemWriter. The writer is free to do any cleanup
     * necessary.
     *
     * @throws Exception is thrown for any errors.
     */
    public void close() throws Exception {
        if (emf != null) {
            em.close();
            emf.close();
        }
    }

    /**
     *
     * Returns the current checkpoint data for this writer.
     * It is called before a chunk checkpoint is committed.
     *
     * @return null
     * @throws Exception upon errors
     */
    public Serializable checkpointInfo() throws Exception {
        return null;
    }
}
