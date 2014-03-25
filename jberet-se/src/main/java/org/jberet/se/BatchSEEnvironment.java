/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.se;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.transaction.TransactionManager;

import org.jberet.se._private.SEBatchLogger;
import org.jberet.spi.ArtifactFactory;
import org.jberet.spi.BatchEnvironment;
import org.jberet.tx.LocalTransactionManager;

/**
 * Represents the Java SE batch runtime environment and its services.
 */
public final class BatchSEEnvironment implements BatchEnvironment {
    ExecutorService executorService;

    public static final String CONFIG_FILE_NAME = "jberet.properties";

    private final Properties configProperties;
    private final TransactionManager tm;

    static final String THREAD_POOL_TYPE = "thread-pool-type";
    static final String THREAD_POOL_TYPE_CACHED = "Cached";
    static final String THREAD_POOL_TYPE_FIXED = "Fixed";
    static final String THREAD_POOL_TYPE_CONFIGURED = "Configured";

    static final String THREAD_POOL_CORE_SIZE = "thread-pool-core-size";
    static final String THREAD_POOL_MAX_SIZE = "thread-pool-max-size";
    static final String THREAD_POOL_KEEP_ALIVE_TIME = "thread-pool-keep-alive-time";
    static final String THREAD_POOL_QUEUE_CAPACITY = "thread-pool-queue-capacity";
    static final String THREAD_POOL_ALLOW_CORE_THREAD_TIMEOUT = "thread-pool-allow-core-thread-timeout";
    static final String THREAD_POOL_PRESTART_ALL_CORE_THREADS = "thread-pool-prestart-all-core-threads";
    static final String THREAD_POOL_REJECTION_POLICY = "thread-pool-rejection-policy";
    static final String THREAD_FACTORY = "thread-factory";

    public BatchSEEnvironment() {
        configProperties = new Properties();
        final InputStream configStream = getClassLoader().getResourceAsStream(CONFIG_FILE_NAME);
        if (configStream != null) {
            try {
                configProperties.load(configStream);
            } catch (IOException e) {
                throw SEBatchLogger.LOGGER.failToLoadConfig(e, CONFIG_FILE_NAME);
            }
        } else {
            SEBatchLogger.LOGGER.useDefaultJBeretConfig(CONFIG_FILE_NAME);
        }
        this.tm = LocalTransactionManager.getInstance();

        createThreadPoolExecutor();
    }

    @Override
    public ClassLoader getClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = BatchSEEnvironment.class.getClassLoader();
        }
        return cl;
    }

    @Override
    public ArtifactFactory getArtifactFactory() {
        return new SEArtifactFactory();
    }

    @Override
    public Future<?> submitTask(final Runnable task) {
        return executorService.submit(task);
    }

    @Override
    public <T> Future<T> submitTask(final Runnable task, final T result) {
        return executorService.submit(task, result);
    }

    @Override
    public <T> Future<T> submitTask(final Callable<T> task) {
        return executorService.submit(task);
    }

    @Override
    public TransactionManager getTransactionManager() {
        return tm;
    }

    @Override
    public Properties getBatchConfigurationProperties() {
        return this.configProperties;
    }

    void createThreadPoolExecutor() {
        String threadPoolType = configProperties.getProperty(THREAD_POOL_TYPE);
        final String threadFactoryProp = configProperties.getProperty(THREAD_FACTORY);
        final ThreadFactory threadFactory;
        if (threadFactoryProp != null && !threadFactoryProp.isEmpty()) {
            try {
                final Class<?> threadFactoryClass = getClassLoader().loadClass(threadFactoryProp.trim());
                threadFactory = (ThreadFactory) threadFactoryClass.newInstance();
            } catch (Exception e) {
                throw SEBatchLogger.LOGGER.failToGetConfigProperty(THREAD_FACTORY, threadFactoryProp, e);
            }
        } else {
            threadFactory = new BatchThreadFactory();
        }

        if (threadPoolType == null || threadPoolType.isEmpty() || threadPoolType.trim().equalsIgnoreCase(THREAD_POOL_TYPE_CACHED)) {
            executorService = Executors.newCachedThreadPool(threadFactory);
            return;
        }

        final String coreSizeProp = configProperties.getProperty(THREAD_POOL_CORE_SIZE);
        final int coreSize;
        try {
            coreSize = Integer.parseInt(coreSizeProp.trim());
        } catch (Exception e) {
            throw SEBatchLogger.LOGGER.failToGetConfigProperty(THREAD_POOL_CORE_SIZE, coreSizeProp, e);
        }

        threadPoolType = threadPoolType.trim();
        if (threadPoolType.equalsIgnoreCase(THREAD_POOL_TYPE_FIXED)) {
            executorService = Executors.newFixedThreadPool(coreSize, threadFactory);
            return;
        }

        if (threadPoolType.equalsIgnoreCase(THREAD_POOL_TYPE_CONFIGURED)) {
            final String maxSizeProp = configProperties.getProperty(THREAD_POOL_MAX_SIZE);
            final int maxSize;
            try {
                maxSize = Integer.parseInt(maxSizeProp.trim());
            } catch (Exception e) {
                throw SEBatchLogger.LOGGER.failToGetConfigProperty(THREAD_POOL_MAX_SIZE, maxSizeProp, e);
            }

            final String keepAliveProp = configProperties.getProperty(THREAD_POOL_KEEP_ALIVE_TIME);
            final long keepAliveSeconds;
            try {
                keepAliveSeconds = Long.parseLong(keepAliveProp.trim());
            } catch (Exception e) {
                throw SEBatchLogger.LOGGER.failToGetConfigProperty(THREAD_POOL_KEEP_ALIVE_TIME, keepAliveProp, e);
            }

            final String queueCapacityProp = configProperties.getProperty(THREAD_POOL_QUEUE_CAPACITY);
            final int queueCapacity;
            try {
                queueCapacity = Integer.parseInt(queueCapacityProp.trim());
            } catch (Exception e) {
                throw SEBatchLogger.LOGGER.failToGetConfigProperty(THREAD_POOL_QUEUE_CAPACITY, queueCapacityProp, e);
            }

            final String allowCoreThreadTimeoutProp = configProperties.getProperty(THREAD_POOL_ALLOW_CORE_THREAD_TIMEOUT);
            final boolean allowCoreThreadTimeout = allowCoreThreadTimeoutProp == null || allowCoreThreadTimeoutProp.isEmpty() ? false :
                    Boolean.parseBoolean(allowCoreThreadTimeoutProp.trim());

            final String prestartAllCoreThreadsProp = configProperties.getProperty(THREAD_POOL_PRESTART_ALL_CORE_THREADS);
            final boolean prestartAllCoreThreads = prestartAllCoreThreadsProp == null || prestartAllCoreThreadsProp.isEmpty() ? false :
                    Boolean.parseBoolean(prestartAllCoreThreadsProp.trim());

            final BlockingQueue<Runnable> workQueue = queueCapacity > 0 ?
                    new LinkedBlockingQueue<Runnable>(queueCapacity) : new SynchronousQueue<Runnable>(true);

            final String rejectionPolicyProp = configProperties.getProperty(THREAD_POOL_REJECTION_POLICY);
            RejectedExecutionHandler rejectionHandler = null;

            if (rejectionPolicyProp != null && !rejectionPolicyProp.isEmpty()) {
                try {
                    final Class<?> aClass = getClassLoader().loadClass(rejectionPolicyProp.trim());
                    rejectionHandler = (RejectedExecutionHandler) aClass.newInstance();
                } catch (Exception e) {
                    throw SEBatchLogger.LOGGER.failToGetConfigProperty(THREAD_POOL_REJECTION_POLICY, rejectionPolicyProp, e);
                }
            }

            final ThreadPoolExecutor threadPoolExecutor = rejectionHandler == null ?
                    new ThreadPoolExecutor(coreSize, maxSize, keepAliveSeconds, TimeUnit.SECONDS, workQueue, threadFactory) :
                    new ThreadPoolExecutor(coreSize, maxSize, keepAliveSeconds, TimeUnit.SECONDS, workQueue, threadFactory, rejectionHandler);

            if (allowCoreThreadTimeout) {
                threadPoolExecutor.allowCoreThreadTimeOut(true);
            }
            if (prestartAllCoreThreads) {
                threadPoolExecutor.prestartAllCoreThreads();
            }
            executorService = threadPoolExecutor;
            return;
        }

        throw SEBatchLogger.LOGGER.failToGetConfigProperty(THREAD_POOL_TYPE, threadPoolType, null);
    }
}
