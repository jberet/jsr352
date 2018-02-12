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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.batch.api.BatchProperty;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.datastax.driver.core.AuthProvider;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.CodecRegistry;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.NettyOptions;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ProtocolOptions;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.SSLOptions;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SocketOptions;
import com.datastax.driver.core.ThreadingOptions;
import com.datastax.driver.core.TimestampGenerator;
import com.datastax.driver.core.policies.AddressTranslator;
import com.datastax.driver.core.policies.LoadBalancingPolicy;
import com.datastax.driver.core.policies.ReconnectionPolicy;
import com.datastax.driver.core.policies.RetryPolicy;
import com.datastax.driver.core.policies.SpeculativeExecutionPolicy;
import org.jberet.support._private.SupportLogger;

/**
 * The base class for {@link CassandraItemReader} and {@link CassandraItemWriter}.
 *
 * @see CassandraItemReader
 * @see CassandraItemWriter
 * @since 1.3.0
 */
public abstract class CassandraReaderWriterBase extends JsonItemReaderWriterBase {
    /**
     * This field holds an optional injection of {@code com.datastax.driver.core.Cluster}.
     * The application may implement a {@code javax.enterprise.inject.Produces} method to satisfy
     * this dependency injection.
     */
    @Inject
    protected Instance<Cluster> clusterInstance;

    /**
     * This field holds an optional injection of {@code com.datastax.driver.core.Session}.
     * The application may implement a {@code javax.enterprise.inject.Produces} method to satisfy
     * this dependency injection.
     */
    @Inject
    protected Instance<Session> sessionInstance;

    /**
     * The CQL statement for reading data from, or inserting data into Cassandra. It should include parameter
     * markers that will be filled in with real data by the current batch {@code ItemReader} or {@code ItemWriter}.
     */
    @Inject
    @BatchProperty
    protected String cql;

    /**
     * For {@code ItemReader}, it's the java type that each data item should be converted to; for {@code ItemWriter},
     * it's the java type for each incoming data item. In either case, the valid values are:
     * <p>
     * <ul>
     * <li>a custom java type that represents data item;
     * <li>java.util.Map
     * <li>java.util.List
     * </ul>
     */
    @Inject
    @BatchProperty
    protected Class beanType;

    /**
     * Cassandra contact points
     */
    @Inject
    @BatchProperty
    protected List<String> contactPoints;

    @Inject
    @BatchProperty
    protected String keyspace;

    /**
     * User name for the Cassandra connection
     */
    @Inject
    @BatchProperty
    protected String user;

    /**
     * Password for the Cassandra connection
     */
    @Inject
    @BatchProperty
    protected String password;

    /**
     * Additional properties for the Cassandra cluster
     */
    @Inject
    @BatchProperty
    protected Map<String, String> clusterProperties;

    protected Cluster cluster;
    protected Session session;
    protected boolean sessionCreated;
    protected PreparedStatement preparedStatement;

    protected void init() throws Exception {
        if (!sessionInstance.isUnsatisfied()) {
            session = sessionInstance.get();
        } else {
            if (!clusterInstance.isUnsatisfied()) {
                cluster = clusterInstance.get();
            } else {
                Cluster.Builder clusterBuilder = Cluster.builder();
                addContactPoints(clusterBuilder);

                if (clusterProperties != null) {
                    applyClusterProperties(clusterBuilder);
                    if (user != null && user.length() > 0) {
                        clusterBuilder.withCredentials(user, password == null ? "" : password);
                    }
                }
                cluster = clusterBuilder.build();
            }
            session = cluster.connect(keyspace);
            sessionCreated = true;
        }
    }

    protected void applyClusterProperties(final Cluster.Builder clusterBuilder) throws Exception {
        for (final Map.Entry<String, String> e : clusterProperties.entrySet()) {
            final String k = e.getKey();
            if (k.equalsIgnoreCase("AddressTranslator")) {
                clusterBuilder.withAddressTranslator(create(e.getValue(), AddressTranslator.class));
            } else if (k.equalsIgnoreCase("AuthProvider")) {
                clusterBuilder.withAuthProvider(create(e.getValue(), AuthProvider.class));
            } else if (k.equalsIgnoreCase("ClusterName")) {
                clusterBuilder.withClusterName(e.getValue());
            } else if (k.equalsIgnoreCase("CodecRegistry")) {
                clusterBuilder.withCodecRegistry(create(e.getValue(), CodecRegistry.class));
            } else if (k.equalsIgnoreCase("Compression")) {
                clusterBuilder.withCompression(
                        ProtocolOptions.Compression.valueOf(e.getValue().toUpperCase(Locale.ENGLISH)));
            } else if (k.equalsIgnoreCase("user")) {
                if (user == null || user.length() == 0) {
                    user = e.getValue();
                }
            } else if (k.equalsIgnoreCase("password")) {
                if (password == null || password.length() == 0) {
                    password = e.getValue();
                }
            } else if (k.equalsIgnoreCase("InitialListeners")) {
                List<Host.StateListener> stateListeners = new ArrayList<>();
                for (String s : e.getValue().split(":")) {
                    stateListeners.add(create(s.trim(), Host.StateListener.class));
                }
                clusterBuilder.withInitialListeners(stateListeners);
            } else if (k.equalsIgnoreCase("LoadBalancingPolicy")) {
                clusterBuilder.withLoadBalancingPolicy(create(e.getValue(), LoadBalancingPolicy.class));
            } else if (k.equalsIgnoreCase("MaxSchemaAgreementWaitSeconds")) {
                clusterBuilder.withMaxSchemaAgreementWaitSeconds(Integer.parseInt(e.getValue()));
            } else if (k.equalsIgnoreCase("NettyOptions")) {
                clusterBuilder.withNettyOptions(create(e.getValue(), NettyOptions.class));
            } else if (k.equalsIgnoreCase("JMXReporting")) {
                if (e.getValue().equalsIgnoreCase("false")) {
                    clusterBuilder.withoutJMXReporting();
                }
            } else if (k.equalsIgnoreCase("Metrics")) {
                if (e.getValue().equalsIgnoreCase("false")) {
                    clusterBuilder.withoutMetrics();
                }
            } else if (k.equalsIgnoreCase("PoolingOptions")) {
                clusterBuilder.withPoolingOptions(create(e.getValue(), PoolingOptions.class));
            } else if (k.equalsIgnoreCase("port")) {
                clusterBuilder.withPort(Integer.parseInt(e.getValue()));
            } else if (k.equalsIgnoreCase("ProtocolVersion")) {
                clusterBuilder.withProtocolVersion(ProtocolVersion.valueOf(e.getValue()));
            } else if (k.equalsIgnoreCase("QueryOptions")) {
                clusterBuilder.withQueryOptions(create(e.getValue(), QueryOptions.class));
            } else if (k.equalsIgnoreCase("ReconnectionPolicy")) {
                clusterBuilder.withReconnectionPolicy(create(e.getValue(), ReconnectionPolicy.class));
            } else if (k.equalsIgnoreCase("RetryPolicy")) {
                clusterBuilder.withRetryPolicy(create(e.getValue(), RetryPolicy.class));
            } else if (k.equalsIgnoreCase("SocketOptions")) {
                clusterBuilder.withSocketOptions(create(e.getValue(), SocketOptions.class));
            } else if (k.equalsIgnoreCase("SpeculativeExecutionPolicy")) {
                clusterBuilder.withSpeculativeExecutionPolicy(create(e.getValue(), SpeculativeExecutionPolicy.class));
            } else if (k.equalsIgnoreCase("SSL")) {
                if (e.getValue().equalsIgnoreCase("true")) {
                    clusterBuilder.withSSL();
                }
            } else if (k.equalsIgnoreCase("SSLOptions")) {
                clusterBuilder.withSSL(create(e.getValue(), SSLOptions.class));
            } else if (k.equalsIgnoreCase("ThreadingOptions")) {
                clusterBuilder.withThreadingOptions(create(e.getValue(), ThreadingOptions.class));
            } else if (k.equalsIgnoreCase("TimestampGenerator")) {
                clusterBuilder.withTimestampGenerator(create(e.getValue(), TimestampGenerator.class));
            } else {
                SupportLogger.LOGGER.ignoreProperties(k, e.getValue());
            }
        }
    }

    private void addContactPoints(final Cluster.Builder clusterBuilder) {
        if (contactPoints != null) {
            for (String e : contactPoints) {
                final int i = e.lastIndexOf(':');
                if (i >= 0) {
                    final String addr = e.substring(0, i);
                    final int port = Integer.parseInt(e.substring(i + 1));
                    clusterBuilder.addContactPointsWithPorts(new InetSocketAddress(addr, port));
                } else {
                    clusterBuilder.addContactPoint(e);
                }
            }
        }
    }

    private static <T> T create(final String className, final Class<T> clazz)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        final Class<?> aClass = CassandraReaderWriterBase.class.getClassLoader().loadClass(className);
        return (T) aClass.newInstance();
    }

    public void close() throws Exception {
        if (sessionCreated && session != null && !session.isClosed()) {
            session.close();
        }
    }
}
