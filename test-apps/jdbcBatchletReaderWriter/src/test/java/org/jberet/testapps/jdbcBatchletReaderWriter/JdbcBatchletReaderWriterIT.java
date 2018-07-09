/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.jdbcBatchletReaderWriter;

import javax.batch.runtime.BatchStatus;

import org.jberet.testapps.common.AbstractIT;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests using the following components from jberet-support:
 * <ul>
 *     <li>{@code jdbcBatchlet}
 *     <li>{@code jdbcItemWriter}
 *     <li>{@code jdbcItemReader}
 *     <li>{@code arrayItemReader}
 *     <li>{@code mockItemWriter}
 * </ul>
 *
 * @since 1.3.0.Final
 */
@Ignore("Need to run database server first in a separate process")
public class JdbcBatchletReaderWriterIT extends AbstractIT {
    private static final String jdbcBatchletReaderWriterJob = "jdbcBatchletReaderWriter.xml";

    private static final String url = "xxx";
    private static final String user = "xxx";
    private static final String password = "xxx";
    static final String resultSetProperties = "resultSetType=TYPE_SCROLL_SENSITIVE";

    static final String parameterNames = "xxx";

    static final String parameterTypes =
            "String\n" +
            ", String\n" +
            ", String\n" +
            ", String\n" +
            ", String\n" +
            ", String\n" +
            ", String\n" +
            ", String\n" +
            ", String\n" +
            ", String\n" +
            ", BigDecimal\n" +
            ", String\n" +
            ", String\n" +
            ", String\n" +
            ", String\n" +
            ", String";

    private static final String readerSql = "select * from xxx";

    private static final String writerSql = "insert into xxx ";

    private static final String sqls = "Create table xxx (\n";

    @Test
    public void jdbcBatchletWriterReader() throws Exception {
        params.setProperty("url", url);
        params.setProperty("user", user);
        params.setProperty("password", password);
        params.setProperty("sqls", sqls);
        params.setProperty("readerSql", readerSql);
        params.setProperty("writerSql", writerSql);
        params.setProperty("resultSetProperties", resultSetProperties);
        params.setProperty("parameterNames", parameterNames);
        params.setProperty("parameterTypes", parameterTypes);

        startJobAndWait(jdbcBatchletReaderWriterJob);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
    }
}
