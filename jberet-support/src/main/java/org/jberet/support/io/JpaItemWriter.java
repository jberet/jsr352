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

import java.util.List;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemWriter;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemWriter} that writes
 * data items with Java Persistence API (JPA).
 *
 * @see JpaItemReader
 * @since 1.3.0
 */
@Named
@Dependent
public class JpaItemWriter extends JpaItemReaderWriterBase implements ItemWriter {

    @Inject
    @BatchProperty
    protected boolean entityTransaction;

    @Override
    public void writeItems(final List<Object> items) throws Exception {
        if (entityTransaction) {
            em.getTransaction().begin();
        }
        for (final Object e : items) {
            em.persist(e);
        }

        if (entityTransaction) {
            em.getTransaction().commit();
        }
    }

}
