/*
 * Copyright (c) 2026 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.se.test;

import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;
import javax.sql.DataSource;

import jakarta.batch.operations.JobOperator;
import org.jberet.operations.AbstractJobOperator;
import org.jberet.repository.JobRepository;
import org.jberet.spi.ArtifactFactory;
import org.jberet.spi.BatchEnvironment;
import org.jberet.spi.JobOperatorContext;
import org.jberet.spi.JobTask;
import org.jberet.spi.JobXmlResolver;

/**
 * Shared scaffolding for the administrative-operations SE tests.
 * <p>
 * The name deliberately does not match surefire's test-class patterns, so this is compiled and
 * used but never run as a test.
 */
final class AdminOpsFixtures {

    private AdminOpsFixtures() {
    }

    /**
     * Returns the batch environment the running Java SE runtime is using, so a test can build a
     * second job operator over a different job repository while reusing the real class loader,
     * artifact factory, job XML resolver and thread pool.
     *
     * @return the live batch environment
     */
    static BatchEnvironment seEnvironment() {
        final JobOperator operator = JobOperatorContext.getJobOperatorContext().getJobOperator();
        return ((AbstractJobOperator) operator).getBatchEnvironment();
    }

    /**
     * Wraps a batch environment so that it hands out a different job repository and is otherwise
     * unchanged.
     *
     * @param delegate   the environment to borrow everything else from
     * @param repository the repository to substitute
     *
     * @return the wrapping environment
     */
    static BatchEnvironment withRepository(final BatchEnvironment delegate, final JobRepository repository) {
        return new BatchEnvironment() {
            @Override
            public ClassLoader getClassLoader() {
                return delegate.getClassLoader();
            }

            @Override
            public ArtifactFactory getArtifactFactory() {
                return delegate.getArtifactFactory();
            }

            @Override
            public void submitTask(final JobTask task) {
                delegate.submitTask(task);
            }

            @Override
            public jakarta.transaction.TransactionManager getTransactionManager() {
                return delegate.getTransactionManager();
            }

            @Override
            public JobRepository getJobRepository() {
                return repository;
            }

            @Override
            public JobXmlResolver getJobXmlResolver() {
                return delegate.getJobXmlResolver();
            }

            @Override
            public Properties getBatchConfigurationProperties() {
                return delegate.getBatchConfigurationProperties();
            }

            @Override
            public String getApplicationName() {
                return delegate.getApplicationName();
            }
        };
    }

    /**
     * A {@link DataSource} over a JDBC URL that can be told to make one particular statement fail.
     */
    static final class FaultInjectingDataSource implements DataSource {

        private final String url;
        private final Properties properties;

        /**
         * When non-null, any prepared statement whose SQL contains this text throws on execution.
         * Set after the fixture data is in place, so only the operation under test is affected.
         */
        volatile String failOnSqlContaining;

        /** Counts how many times the fault actually fired, so a test can assert it did. */
        volatile int faultsFired;

        FaultInjectingDataSource(final String url) {
            this.url = url;
            this.properties = new Properties();
        }

        @Override
        public Connection getConnection() throws SQLException {
            return wrap(DriverManager.getConnection(url, properties));
        }

        @Override
        public Connection getConnection(final String username, final String password) throws SQLException {
            return getConnection();
        }

        private Connection wrap(final Connection connection) {
            final InvocationHandler handler = (proxy, method, args) -> {
                if ("prepareStatement".equals(method.getName()) && args != null && args.length > 0
                        && args[0] instanceof String) {
                    final String sql = (String) args[0];
                    final PreparedStatement statement = (PreparedStatement) invoke(method, connection, args);
                    final String trigger = failOnSqlContaining;
                    if (trigger != null && sql.contains(trigger)) {
                        return failing(statement, sql);
                    }
                    return statement;
                }
                return invoke(method, connection, args);
            };
            return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{Connection.class}, handler);
        }

        private PreparedStatement failing(final PreparedStatement statement, final String sql) {
            final InvocationHandler handler = (proxy, method, args) -> {
                final String name = method.getName();
                if (name.startsWith("execute")) {
                    faultsFired++;
                    throw new SQLException("injected failure executing: " + sql);
                }
                return invoke(method, statement, args);
            };
            return (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{PreparedStatement.class}, handler);
        }

        private static Object invoke(final Method method, final Object target, final Object[] args)
                throws Throwable {
            try {
                return method.invoke(target, args);
            } catch (final InvocationTargetException e) {
                throw e.getCause();
            }
        }

        @Override
        public PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(final PrintWriter out) {
        }

        @Override
        public void setLoginTimeout(final int seconds) {
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }

        @Override
        public <T> T unwrap(final Class<T> iface) throws SQLException {
            if (iface.isInstance(this)) {
                return iface.cast(this);
            }
            throw new SQLException("not a wrapper for " + iface);
        }

        @Override
        public boolean isWrapperFor(final Class<?> iface) {
            return iface.isInstance(this);
        }
    }
}
