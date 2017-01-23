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
    /**
     * Flag to control whether to begin entity transaction before writing items,
     * and to commit entity transaction after writing items.
     * Optional property, and defaults to {@code false}.
     */
    @Inject
    @BatchProperty
    protected boolean entityTransaction;

    /**
     * {@inheritDoc}
     * <p>
     * In this method, the entity manager persists the {@code items}.
     * If {@link #entityTransaction} is true, this method explicitly
     * begins entity transaction before writing, and commit it after
     * writing.
     *
     * @param items items to write
     * @throws Exception upon errors
     */
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
