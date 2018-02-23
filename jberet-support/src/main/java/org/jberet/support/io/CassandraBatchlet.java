/*
 * Copyright (c) 2018 Red Hat, Inc. and/or its affiliates.
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

import javax.batch.api.Batchlet;
import javax.enterprise.context.Dependent;
import javax.inject.Named;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import org.jberet.support._private.SupportLogger;

@Named
@Dependent
public class CassandraBatchlet extends CassandraReaderWriterBase implements Batchlet {
    /**
     * {@inheritDoc}
     */
    @Override
    public String process() throws Exception {
        String result = null;
        try {
            initSession();
            final ResultSet resultSet = session.execute(cql);
            final Row one = resultSet.one();
            if (one != null) {
                result = one.toString();
            }
        } finally {
            try {
                close();
            } catch (Exception e) {
                SupportLogger.LOGGER.failToClose(e, session == null ? null : session.toString());
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() throws Exception {
    }
}
