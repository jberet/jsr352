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

import java.io.File;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.jms.ConnectionFactory;
import javax.jms.Queue;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.remoting.impl.netty.NettyAcceptorFactory;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.hornetq.jms.server.config.ConnectionFactoryConfiguration;
import org.hornetq.jms.server.config.JMSConfiguration;
import org.hornetq.jms.server.config.JMSQueueConfiguration;
import org.hornetq.jms.server.config.impl.ConnectionFactoryConfigurationImpl;
import org.hornetq.jms.server.config.impl.JMSConfigurationImpl;
import org.hornetq.jms.server.config.impl.JMSQueueConfigurationImpl;
import org.hornetq.jms.server.embedded.EmbeddedJMS;
import org.jberet.runtime.JobExecutionImpl;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class JmsReaderWriterTest {
    private final JobOperator jobOperator = BatchRuntime.getJobOperator();
    static final String writerTestJobName = "org.jberet.support.io.JmsWriterTest.xml";
    static final String readerTestJobName = "org.jberet.support.io.JmsReaderTest.xml";

    static final String destinationLookupName = "/queue/queue1";
    static final String connectionFactoryLookupName = "/cf";

    protected static EmbeddedJMS jmsServer;

    @BeforeClass
    public static void beforeClass() throws Exception {
        // Step 1. Create HornetQ core configuration, and set the properties accordingly
        Configuration configuration = new ConfigurationImpl();
        configuration.setPersistenceEnabled(false);
        configuration.setJournalDirectory("target/data/journal");
        configuration.setSecurityEnabled(false);
        configuration.getAcceptorConfigurations()
                .add(new TransportConfiguration(NettyAcceptorFactory.class.getName()));

        TransportConfiguration connectorConfig = new TransportConfiguration(NettyConnectorFactory.class.getName());

        configuration.getConnectorConfigurations().put("connector", connectorConfig);

        // Step 2. Create the JMS configuration
        JMSConfiguration jmsConfig = new JMSConfigurationImpl();

        // Step 3. Configure the JMS ConnectionFactory
        ArrayList<String> connectorNames = new ArrayList<String>();
        connectorNames.add("connector");
        ConnectionFactoryConfiguration cfConfig = new ConnectionFactoryConfigurationImpl("cf", false, connectorNames, connectionFactoryLookupName);
        jmsConfig.getConnectionFactoryConfigurations().add(cfConfig);

        // Step 4. Configure the JMS Queue
        JMSQueueConfiguration queueConfig = new JMSQueueConfigurationImpl("queue1", null, false, destinationLookupName);
        jmsConfig.getQueueConfigurations().add(queueConfig);

        // Step 5. Start the JMS Server using the HornetQ core server and the JMS configuration
        jmsServer = new EmbeddedJMS();
        jmsServer.setConfiguration(configuration);
        jmsServer.setJmsConfiguration(jmsConfig);
        jmsServer.start();
        System.out.println("Started Embedded JMS Server");

        // Step 6. Lookup JMS resources defined in the configuration
        JmsResourceProducer.connectionFactory = (ConnectionFactory) jmsServer.lookup(connectionFactoryLookupName);
        JmsResourceProducer.queue = (Queue) jmsServer.lookup(destinationLookupName);

    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (jmsServer != null) {
            jmsServer.stop();
        }
    }

    @Test
    public void readIBMStockTradeCsvWriteJmsBeanType() throws Exception {
        testWrite0(writerTestJobName, StockTrade.class, ExcelWriterTest.ibmStockTradeHeader,
                "0", "10");

        // CsvItemReaderWriter uses header "Date, Time, Open, ..."
        // CsvItemReaderWriter has nameMapping "date, time, open, ..." to match java fields in StockTrade. CsvItemReaderWriter
        // does not understand Jackson mapping annotations in POJO.

        //testRead0(readerTestJobName, StockTrade.class, "readIBMStockTradeCsvWriteJmsBeanType.out",
        //        ExcelWriterTest.ibmStockTradeNameMapping, ExcelWriterTest.ibmStockTradeHeader,
        //        ExcelReaderTest.ibmStockTradeFullExpected, null);
    }


    void testWrite0(final String jobName, final Class<?> beanType, final String csvNameMapping,
                    final String start, final String end) throws Exception {
        final Properties params = CsvItemReaderWriterTest.createParams(CsvProperties.BEAN_TYPE_KEY, beanType.getName());

        if (csvNameMapping != null) {
            params.setProperty("nameMapping", csvNameMapping);
        }
        if (start != null) {
            params.setProperty("start", start);
        }
        if (end != null) {
            params.setProperty("end", end);
        }

        //params.setProperty("connectionFactoryLookupName", connectionFactoryLookupName);
        //params.setProperty("destinationLookupName", destinationLookupName);

        final long jobExecutionId = jobOperator.start(jobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(CsvItemReaderWriterTest.waitTimeoutMinutes, TimeUnit.HOURS);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
    }

    void testRead0(final String jobName, final Class<?> beanType, final String writeResource,
                   final String csvNameMapping, final String csvHeader,
                   final String expect, final String forbid) throws Exception {
        final Properties params = CsvItemReaderWriterTest.createParams(CsvProperties.BEAN_TYPE_KEY, beanType.getName());

        final File writeResourceFile;
        if (writeResource != null) {
            writeResourceFile = new File(CsvItemReaderWriterTest.tmpdir, writeResource);
            params.setProperty("writeResource", writeResourceFile.getPath());
        } else {
            throw new RuntimeException("writeResource is null");
        }
        if (csvNameMapping != null) {
            params.setProperty("nameMapping", csvNameMapping);
        }
        if (csvHeader != null) {
            params.setProperty("header", csvHeader);
        }

        final long jobExecutionId = jobOperator.start(jobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(CsvItemReaderWriterTest.waitTimeoutMinutes, TimeUnit.HOURS);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        CsvItemReaderWriterTest.validate(writeResourceFile, expect, forbid);
    }
}
