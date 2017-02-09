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

import java.util.Map;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.batch.api.BatchProperty;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.jberet.support._private.SupportMessages;

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

    @PostConstruct
    protected void postConstruct() {
        initEntityManager();
    }

    @PreDestroy
    protected void preDestroy() {
        closeEntityManager();
    }

    protected void initEntityManager() {
        if (em == null) {
            if (entityManagerLookupName != null) {
                InitialContext ic = null;
                try {
                    ic = new InitialContext();
                    em = (EntityManager) ic.lookup(entityManagerLookupName);
                } catch (final NamingException e) {
                    throw SupportMessages.MESSAGES.failToLookup(e, entityManagerLookupName);
                } finally {
                    if (ic != null) {
                        try {
                            ic.close();
                        } catch (final NamingException e) {
                            //ignore
                        }
                    }
                }
            } else {
                if (entityManagerInstance != null && !entityManagerInstance.isUnsatisfied()) {
                    em = entityManagerInstance.get();
                }
                if (em == null) {
                    emf = Persistence.createEntityManagerFactory(persistenceUnitName, persistenceUnitProperties);
                    em = emf.createEntityManager();
                }
            }
        }
    }

    protected void closeEntityManager() {
        if (emf != null) {
            em.close();
            emf.close();
        }
    }
}
