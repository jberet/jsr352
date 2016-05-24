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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.jms.ConnectionFactory;
import javax.jms.Queue;

import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyAcceptorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.jms.server.config.ConnectionFactoryConfiguration;
import org.apache.activemq.artemis.jms.server.config.JMSConfiguration;
import org.apache.activemq.artemis.jms.server.config.JMSQueueConfiguration;
import org.apache.activemq.artemis.jms.server.config.impl.ConnectionFactoryConfigurationImpl;
import org.apache.activemq.artemis.jms.server.config.impl.JMSConfigurationImpl;
import org.apache.activemq.artemis.jms.server.config.impl.JMSQueueConfigurationImpl;
import org.apache.activemq.artemis.jms.server.embedded.EmbeddedJMS;
import org.jberet.runtime.JobExecutionImpl;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class JmsReaderWriterTest {
    private static final JobOperator jobOperator = BatchRuntime.getJobOperator();
    static final String writerTestJobName = "org.jberet.support.io.JmsWriterTest.xml";
    static final String readerTestJobName = "org.jberet.support.io.JmsReaderTest.xml";

    static final String destinationLookupName = "/queue/queue1";
    static final String connectionFactoryLookupName = "/cf";

    protected static EmbeddedJMS jmsServer;

    static final String ibmStockTradeExpected1_10 = "09:30, 67040, 09:31, 10810,    09:39, 2500";
    static final String ibmStockTradeForbid1_10 = "09:40";

    static final String ibmStockTradeCellProcessorsDateAsString =
            "null; null; ParseDouble; ParseDouble; ParseDouble; ParseDouble; ParseDouble";

    @BeforeClass
    public static void beforeClass() throws Exception {
        // Step 1. Create HornetQ core configuration, and set the properties accordingly
        final Configuration configuration = new ConfigurationImpl();
        configuration.setPersistenceEnabled(false);
        configuration.setJournalDirectory("target/data/journal");
        configuration.setSecurityEnabled(false);
        configuration.getAcceptorConfigurations()
                .add(new TransportConfiguration(NettyAcceptorFactory.class.getName()));

        final TransportConfiguration connectorConfig = new TransportConfiguration(NettyConnectorFactory.class.getName());

        configuration.getConnectorConfigurations().put("connector", connectorConfig);

        // Step 2. Create the JMS configuration
        final JMSConfiguration jmsConfig = new JMSConfigurationImpl();

        // Step 3. Configure the JMS ConnectionFactory
        final ArrayList<String> connectorNames = new ArrayList<String>();
        connectorNames.add("connector");
        final ConnectionFactoryConfiguration cfConfig = new ConnectionFactoryConfigurationImpl().setName("cf")
                .setConnectorNames(connectorNames).setBindings(connectionFactoryLookupName);
        jmsConfig.getConnectionFactoryConfigurations().add(cfConfig);

        // Step 4. Configure the JMS Queue
        final JMSQueueConfiguration queueConfig = new JMSQueueConfigurationImpl().setName("queue1")
                .setDurable(false).setBindings(destinationLookupName);
        jmsConfig.getQueueConfigurations().add(queueConfig);

        // Step 5. Start the JMS Server using the HornetQ core server and the JMS configuration
        jmsServer = new EmbeddedJMS();
        jmsServer.setConfiguration(configuration);
        jmsServer.setJmsConfiguration(jmsConfig);
        jmsServer.start();
        System.out.println("Started Embedded JMS Server");

        // Step 6. Lookup JMS resources defined in the configuration
        MessagingResourceProducer.connectionFactory = (ConnectionFactory) jmsServer.lookup(connectionFactoryLookupName);
        MessagingResourceProducer.queue = (Queue) jmsServer.lookup(destinationLookupName);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (jmsServer != null) {
            jmsServer.stop();
        }
    }

    @Test
    public void readIBMStockTradeCsvWriteJmsBeanType() throws Exception {
        testWrite0(writerTestJobName, StockTrade.class, ExcelWriterTest.ibmStockTradeHeader, ExcelWriterTest.ibmStockTradeCellProcessors,
                "1", "10");

        // CsvItemReaderWriter uses header "Date, Time, Open, ..."
        // CsvItemReaderWriter has nameMapping "date, time, open, ..." to match java fields in StockTrade. CsvItemReaderWriter
        // does not understand Jackson mapping annotations in POJO.

        testRead0(readerTestJobName, StockTrade.class, "readIBMStockTradeCsvWriteJmsBeanType.out",
                ExcelWriterTest.ibmStockTradeNameMapping, ExcelWriterTest.ibmStockTradeHeader,
                ibmStockTradeExpected1_10, ibmStockTradeForbid1_10);
    }

    @Test
    public void readIBMStockTradeCsvWriteJmsMapType() throws Exception {
        testWrite0(writerTestJobName, Map.class, ExcelWriterTest.ibmStockTradeHeader, ibmStockTradeCellProcessorsDateAsString,
                "1", "10");

        testRead0(readerTestJobName, Map.class, "readIBMStockTradeCsvWriteJmsMapType.out",
                ExcelWriterTest.ibmStockTradeNameMapping, ExcelWriterTest.ibmStockTradeHeader,
                ibmStockTradeExpected1_10, ibmStockTradeForbid1_10);
    }

    @Test
    public void readIBMStockTradeCsvWriteJmsListType() throws Exception {
        testWrite0(writerTestJobName, List.class, ExcelWriterTest.ibmStockTradeHeader, ExcelWriterTest.ibmStockTradeCellProcessors,
                "1", "10");

        testRead0(readerTestJobName, List.class, "readIBMStockTradeCsvWriteJmsListType.out",
                ExcelWriterTest.ibmStockTradeNameMapping, ExcelWriterTest.ibmStockTradeHeader,
                ibmStockTradeExpected1_10, ibmStockTradeForbid1_10);
    }


    static void testWrite0(final String jobName, final Class<?> beanType, final String csvNameMapping, final String cellProcessors,
                    final String start, final String end) throws Exception {
        final Properties params = CsvItemReaderWriterTest.createParams(CsvProperties.BEAN_TYPE_KEY, beanType.getName());

        if (csvNameMapping != null) {
            params.setProperty("nameMapping", csvNameMapping);
        }
        if (cellProcessors != null) {
            params.setProperty("cellProcessors", cellProcessors);
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

    static void testRead0(final String jobName, final Class<?> beanType, final String writeResource,
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
