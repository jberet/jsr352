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
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemReader;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.Query;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemReader} that reads
 * data with Java Persistence API (JPA).
 *
 * @see JpaItemWriter
 * @since 1.3.0
 */
@Named
@Dependent
public class JpaItemReader extends JpaItemReaderWriterBase implements ItemReader {

    @Inject
    protected Instance<Query> queryInstance;

    @Inject
    @BatchProperty
    protected String jpaQuery;

    @Inject
    @BatchProperty
    protected String namedQuery;

    @Inject
    @BatchProperty
    protected String nativeQuery;

    @Inject
    @BatchProperty
    protected String storedProcedureQuery;

    @Inject
    @BatchProperty
    protected String namedStoredProcedureQuery;

    @Inject
    @BatchProperty
    protected Class beanType;

    @Inject
    @BatchProperty
    protected String resultSetMapping;

    @Inject
    @BatchProperty
    protected int firstResult;

    @Inject
    @BatchProperty
    protected int maxResults;

    protected Query query;

    @Override
    public void open(final Serializable checkpoint) throws Exception {
        super.open(checkpoint);
        query = getQuery();
    }

    @Override
    public Object readItem() throws Exception {
        return null;
    }

    protected Query getQuery() {
        Query q = null;
        if (queryInstance != null && !queryInstance.isUnsatisfied()) {
            q = queryInstance.get();
        }
        if (q == null) {
            if (jpaQuery != null) {
                q = beanType != null ? em.createQuery(jpaQuery, beanType) :
                        em.createQuery(jpaQuery);
            } else if (namedQuery != null) {
                q = beanType != null ? em.createNamedQuery(namedQuery, beanType) :
                        em.createNamedQuery(namedQuery);
            } else if (nativeQuery != null) {
                q = beanType != null ? em.createNativeQuery(nativeQuery, beanType) :
                        resultSetMapping != null ? em.createNativeQuery(nativeQuery, resultSetMapping) :
                        em.createNativeQuery(nativeQuery);
            } else if (storedProcedureQuery != null) {
                q = beanType != null ? em.createStoredProcedureQuery(storedProcedureQuery, beanType) :
                        resultSetMapping != null ? em.createStoredProcedureQuery(storedProcedureQuery, resultSetMapping) :
                        em.createStoredProcedureQuery(storedProcedureQuery);
            } else if (namedStoredProcedureQuery != null) {
                q = em.createNamedStoredProcedureQuery(namedStoredProcedureQuery);
            }
        }

        if (firstResult != 0) {
            q.setFirstResult(firstResult);
        }
        if (maxResults != 0) {
            q.setMaxResults(maxResults);
        }

        return q;
    }

}
