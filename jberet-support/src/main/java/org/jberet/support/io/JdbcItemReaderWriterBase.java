/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.batch.api.BatchProperty;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.jberet.support._private.SupportLogger;

/**
 * The base class for {@link JdbcItemReader} and {@link JdbcItemWriter}.
 *
 * @see     JdbcItemReader
 * @see     JdbcItemWriter
 * @since   1.1.0
 */
public abstract class JdbcItemReaderWriterBase extends JsonItemReaderWriterBase {
    /**
     * The sql statement for reading data from database, or inserting data into database. It should include parameter
     * markers that will be filled in with real data by the current batch {@code ItemReader} or {@code ItemWriter}.
     */
    @Inject
    @BatchProperty
    protected String sql;

    /**
     * For {@code ItemReader}, it's the java type that each data item should be converted to; for {@code ItemWriter},
     * it's the java type for each incoming data item. In either case, the valid values are:
     * <p>
     *     <ul>
     *         <li>a custom java type that represents data item; </li>
     *         <li>java.util.Map</li>
     *         <li>java.util.List</li>
     *     </ul>
     * </p>
     */
    @Inject
    @BatchProperty
    protected Class beanType;

    /**
     * JNDI lookup name of the {@code javax.sql.DataSource}. Optional property, and defaults to null. If specified,
     * it will be used to look up the target {@code DataSource}, and other database connection batch properties for
     * this writer class will be ignored.
     */
    @Inject
    @BatchProperty
    protected String dataSourceLookup;

    /**
     * JDBC connection url
     */
    @Inject
    @BatchProperty
    protected String url;

    /**
     * User name for the JDBC connection
     */
    @Inject
    @BatchProperty
    protected String user;

    /**
     * Password for the JDBC connection
     */
    @Inject
    @BatchProperty
    protected String password;

    /**
     * Additional properties for the JDBC connection
     */
    @Inject
    @BatchProperty
    protected Map<String, String> properties;

    protected PreparedStatement preparedStatement;
    protected DataSource dataSource;
    private Properties dbProperties;

    protected void init() throws Exception {
        if (dataSourceLookup != null) {
            dataSource = InitialContext.doLookup(dataSourceLookup);
        } else {
            dbProperties = new Properties();
            if (properties != null) {
                dbProperties.putAll(properties);
            }
            if (user != null) {
                dbProperties.put("user", user.trim());
            }
            if (password != null) {
                dbProperties.put("password", password.trim());
            }
        }
        if (beanType != List.class && beanType != Map.class) {
            initJsonFactoryAndObjectMapper();
        }
    }

    protected Connection getConnection() throws Exception {
        if (dataSource != null) {
            return dataSource.getConnection();
        } else {
            return DriverManager.getConnection(url, dbProperties);
        }
    }

    protected static void close(final Connection connection, final PreparedStatement preparedStatement) {
        if (preparedStatement != null) {
            try {
                preparedStatement.close();
            } catch (final SQLException e) {
                SupportLogger.LOGGER.tracef(e, "Failed to close PreparedStatement");
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (final SQLException e) {
                SupportLogger.LOGGER.tracef(e, "Failed to close connection.");
            }
        }
    }
}
