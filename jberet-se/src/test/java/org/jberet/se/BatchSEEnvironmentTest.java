/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.se;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import jakarta.batch.operations.BatchRuntimeException;

import org.jberet.repository.JdbcRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.jberet.se.BatchSEEnvironment.THREAD_FACTORY;
import static org.jberet.se.BatchSEEnvironment.THREAD_POOL_ALLOW_CORE_THREAD_TIMEOUT;
import static org.jberet.se.BatchSEEnvironment.THREAD_POOL_CORE_SIZE;
import static org.jberet.se.BatchSEEnvironment.THREAD_POOL_KEEP_ALIVE_TIME;
import static org.jberet.se.BatchSEEnvironment.THREAD_POOL_MAX_SIZE;
import static org.jberet.se.BatchSEEnvironment.THREAD_POOL_PRESTART_ALL_CORE_THREADS;
import static org.jberet.se.BatchSEEnvironment.THREAD_POOL_QUEUE_CAPACITY;
import static org.jberet.se.BatchSEEnvironment.THREAD_POOL_REJECTION_POLICY;
import static org.jberet.se.BatchSEEnvironment.THREAD_POOL_TYPE;
import static org.jberet.se.BatchSEEnvironment.THREAD_POOL_TYPE_CONFIGURED;
import static org.jberet.se.BatchSEEnvironment.THREAD_POOL_TYPE_FIXED;

public class BatchSEEnvironmentTest {
    private BatchSEEnvironment batchEnvironment = new BatchSEEnvironment();

    @Test
    public void testCreateThreadPoolExecutor() throws Exception {
        final Properties configProperties = batchEnvironment.getBatchConfigurationProperties();
        final Class<? extends ThreadFactory> defaultThreadFactoryClass = BatchThreadFactory.class;
        final Class<? extends RejectedExecutionHandler> defaultRejectionHandlerClass = ThreadPoolExecutor.AbortPolicy.class;
        //when jberet-se/src/test/resources/jberet.properties contains no thread-pool related properties,
        //Cached thread-pool is used, with default values
        verifyThreadPool(0, Integer.MAX_VALUE, 60L, 0, defaultThreadFactoryClass, false, defaultRejectionHandlerClass, batchEnvironment.createThreadPoolExecutor());

        final Class<? extends ThreadFactory> threadFactoryClass = SimpleThreadFactory.class;
        final int coreSize = 10;
        final int maxSize = 100;
        final long keepAliveTime = 600;
        final int queueCapacity = 300;

        //a Cached thread-pool, with a thread-factory, coreSize (ignored), maxSize (ignored), keepAliveTime (ignored),
        //queueCapacity (ignored), allowCoreThreadTimeout (ignored)
        configProperties.setProperty(THREAD_FACTORY, threadFactoryClass.getName());
        configProperties.setProperty(THREAD_POOL_CORE_SIZE, String.valueOf(coreSize));
        configProperties.setProperty(THREAD_POOL_MAX_SIZE, String.valueOf(maxSize));
        configProperties.setProperty(THREAD_POOL_KEEP_ALIVE_TIME, String.valueOf(keepAliveTime));
        configProperties.setProperty(THREAD_POOL_QUEUE_CAPACITY, String.valueOf(queueCapacity));
        configProperties.setProperty(THREAD_POOL_ALLOW_CORE_THREAD_TIMEOUT, String.valueOf(true));
        verifyThreadPool(0, Integer.MAX_VALUE, 60L, 0, threadFactoryClass, false, defaultRejectionHandlerClass, batchEnvironment.createThreadPoolExecutor());

        //a Fixed thread-pool, with a thread-factory, coreSize (ignored), maxSize (ignored), keepAliveTime (ignored),
        //queueCapacity (ignored), allowCoreThreadTimeout (ignored)
        configProperties.setProperty(THREAD_POOL_TYPE, THREAD_POOL_TYPE_FIXED);
        verifyThreadPool(coreSize, coreSize, 0L, Integer.MAX_VALUE, threadFactoryClass, false, defaultRejectionHandlerClass, batchEnvironment.createThreadPoolExecutor());

        //a Fixed thread-pool, with default thread-factory
        configProperties.setProperty(THREAD_FACTORY, defaultThreadFactoryClass.getName());
        verifyThreadPool(coreSize, coreSize, 0L, Integer.MAX_VALUE, defaultThreadFactoryClass, false, defaultRejectionHandlerClass, batchEnvironment.createThreadPoolExecutor());

        //a Configured thread-pool, with a thread-factory, coreSize (ignored), maxSize (ignored), keepAliveTime (ignored),
        //queueCapacity (ignored), allowCoreThreadTimeout (ignored)
        configProperties.setProperty(THREAD_POOL_TYPE, THREAD_POOL_TYPE_CONFIGURED);
        verifyThreadPool(coreSize, maxSize, keepAliveTime, queueCapacity, defaultThreadFactoryClass, true, defaultRejectionHandlerClass, batchEnvironment.createThreadPoolExecutor());

        //a Configured thread-pool, with a custom thread-factory, allowCoreThreadTimeout false
        configProperties.setProperty(THREAD_FACTORY, threadFactoryClass.getName());
        configProperties.setProperty(THREAD_POOL_ALLOW_CORE_THREAD_TIMEOUT, String.valueOf(false));
        verifyThreadPool(coreSize, maxSize, keepAliveTime, queueCapacity, threadFactoryClass, false, defaultRejectionHandlerClass, batchEnvironment.createThreadPoolExecutor());

        //a Configured thread-pool, with prestartCore true
        configProperties.setProperty(THREAD_POOL_PRESTART_ALL_CORE_THREADS, String.valueOf(true));
        final ThreadPoolExecutor threadPoolExecutor =
                verifyThreadPool(coreSize, maxSize, keepAliveTime, queueCapacity, threadFactoryClass, false, defaultRejectionHandlerClass, batchEnvironment.createThreadPoolExecutor());
        Assertions.assertEquals(coreSize, threadPoolExecutor.getPoolSize());

        //a Configured thread-pool, with custom rejection policy
        configProperties.setProperty(THREAD_POOL_REJECTION_POLICY, SimpleRejectionHandler.class.getName());
        verifyThreadPool(coreSize, maxSize, keepAliveTime, queueCapacity, threadFactoryClass, false, SimpleRejectionHandler.class, batchEnvironment.createThreadPoolExecutor());


        //a Configured thread-pool, with missing coreSize
        configProperties.remove(THREAD_POOL_CORE_SIZE);
        try {
            batchEnvironment.createThreadPoolExecutor();
            Assertions.fail("Expecting exception, but got no exception when missing property " + THREAD_POOL_CORE_SIZE);
        } catch (BatchRuntimeException e) {
            System.out.printf("Got the expected %s%n", e);
        }
        //a Configured thread-pool, with missing maxSize
        configProperties.setProperty(THREAD_POOL_CORE_SIZE, String.valueOf(coreSize));
        configProperties.remove(THREAD_POOL_MAX_SIZE);
        try {
            batchEnvironment.createThreadPoolExecutor();
            Assertions.fail("Expecting exception, but got no exception when missing property " + THREAD_POOL_MAX_SIZE);
        } catch (BatchRuntimeException e) {
            System.out.printf("Got the expected %s%n", e);
        }
        //a Configured thread-pool, with missing keepAliveTime
        configProperties.setProperty(THREAD_POOL_CORE_SIZE, String.valueOf(coreSize));
        configProperties.setProperty(THREAD_POOL_MAX_SIZE, String.valueOf(maxSize));
        configProperties.remove(THREAD_POOL_KEEP_ALIVE_TIME);
        try {
            batchEnvironment.createThreadPoolExecutor();
            Assertions.fail("Expecting exception, but got no exception when missing property " + THREAD_POOL_KEEP_ALIVE_TIME);
        } catch (BatchRuntimeException e) {
            System.out.printf("Got the expected %s%n", e);
        }

        //a Configured thread-pool, with missing queueCapacity
        configProperties.setProperty(THREAD_POOL_CORE_SIZE, String.valueOf(coreSize));
        configProperties.setProperty(THREAD_POOL_MAX_SIZE, String.valueOf(maxSize));
        configProperties.setProperty(THREAD_POOL_KEEP_ALIVE_TIME, String.valueOf(keepAliveTime));
        configProperties.remove(THREAD_POOL_QUEUE_CAPACITY);
        try {
            batchEnvironment.createThreadPoolExecutor();
            Assertions.fail("Expecting exception, but got no exception when missing property " + THREAD_POOL_QUEUE_CAPACITY);
        } catch (BatchRuntimeException e) {
            System.out.printf("Got the expected %s%n", e);
        }

        //invalid thread-pool type
        configProperties.setProperty(THREAD_POOL_TYPE, "xxx");
        try {
            batchEnvironment.createThreadPoolExecutor();
            Assertions.fail("Expecting exception, but got no exception when specifying invalid property " + THREAD_POOL_TYPE);
        } catch (BatchRuntimeException e) {
            System.out.printf("Got the expected %s%n", e);
        }
    }

    @Test
    public void testResolveDbProps() {
        BatchSEEnvironment batchSEObject = new BatchSEEnvironment();
            Assertions.assertEquals("foopass",
                batchSEObject
                        .getBatchConfigurationProperties()
                        .getProperty(org.jberet.repository.JdbcRepository.DB_PASSWORD_KEY));

        Assertions.assertEquals("foouser",
                batchSEObject
                        .getBatchConfigurationProperties()
                        .getProperty(JdbcRepository.DB_USER_KEY));
    }

    @Test
    public void testPropParsings() {
        String p1 = "${FOO:bar}";
        String p2 = "${FOO}";
        String p3 = "${BAR:defaultVal}";
        String p4 = "${INVALID";
        String p5 = "$INVALID}";
        String p6 = "${{INVALID}";
        String p7 = "${INVALID}}";
        String p8 = "${BAR:}";
        String p9 = "${}";
        String p10 = "${BAR}";


        Assertions.assertEquals("foo", BatchSEEnvironment.parseProp(p1));
        Assertions.assertEquals("foo", BatchSEEnvironment.parseProp(p2));
        Assertions.assertEquals("defaultVal", BatchSEEnvironment.parseProp(p3));
        Assertions.assertEquals("${INVALID", BatchSEEnvironment.parseProp(p4));
        Assertions.assertEquals("$INVALID}", BatchSEEnvironment.parseProp(p5));
        Assertions.assertEquals("${{INVALID}", BatchSEEnvironment.parseProp(p6));
        Assertions.assertEquals("${INVALID}}", BatchSEEnvironment.parseProp(p7));
        Assertions.assertEquals("", BatchSEEnvironment.parseProp(p8));
        Assertions.assertEquals("${}", BatchSEEnvironment.parseProp(p9));
        Assertions.assertNull(BatchSEEnvironment.parseProp(p10));
    }

    private ThreadPoolExecutor verifyThreadPool(final int coreSize,
                                  final int maxSize,
                                  final long keepAliveTime,
                                  final int queueCapacity,
                                  final Class<? extends ThreadFactory> threadFactoryClass,
                                  final boolean allowCoreThreadTimeout,
                                  final Class<? extends RejectedExecutionHandler> rejectionHandlerClass,
                                  final ExecutorService executorService) {
        final ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executorService;
        Assertions.assertEquals(coreSize, threadPoolExecutor.getCorePoolSize());
        Assertions.assertEquals(maxSize, threadPoolExecutor.getMaximumPoolSize());
        Assertions.assertEquals(keepAliveTime, threadPoolExecutor.getKeepAliveTime(TimeUnit.SECONDS));
        Assertions.assertEquals(queueCapacity, threadPoolExecutor.getQueue().remainingCapacity());
        Assertions.assertEquals(threadFactoryClass, threadPoolExecutor.getThreadFactory().getClass());
        Assertions.assertEquals(allowCoreThreadTimeout, threadPoolExecutor.allowsCoreThreadTimeOut());
        Assertions.assertEquals(rejectionHandlerClass, threadPoolExecutor.getRejectedExecutionHandler().getClass());
        return threadPoolExecutor;
    }

    static class SimpleThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(final Runnable r) {
            return new Thread(r);
        }
    }

    static class SimpleRejectionHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(final Runnable r, final ThreadPoolExecutor executor) {
            System.out.printf("In %s, runnalbe is %s, executor is %s%n", this, r, executor);
        }
    }
}
