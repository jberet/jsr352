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

import java.util.List;
import java.util.Map;

import org.hornetq.api.core.Message;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.api.core.client.SendAcknowledgementHandler;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.core.server.HornetQServers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.jberet.support.io.JmsReaderWriterTest.ibmStockTradeCellProcessorsDateAsString;
import static org.jberet.support.io.JmsReaderWriterTest.ibmStockTradeExpected1_10;
import static org.jberet.support.io.JmsReaderWriterTest.ibmStockTradeForbid1_10;
import static org.jberet.support.io.JmsReaderWriterTest.testRead0;
import static org.jberet.support.io.JmsReaderWriterTest.testWrite0;

public class HornetQReaderWriterTest {
    static final String writerTestJobName = "org.jberet.support.io.HornetQWriterTest.xml";
    static final String readerTestJobName = "org.jberet.support.io.HornetQReaderTest.xml";

    //static final String connectorFactoryName = "org.hornetq.core.remoting.impl.netty.NettyConnectorFactory";
    //static final String acceptorFactoryName = "org.hornetq.core.remoting.impl.netty.NettyAcceptorFactory";

    static final String connectorFactoryName = "org.hornetq.core.remoting.impl.invm.InVMConnectorFactory";
    static final String acceptorFactoryName = "org.hornetq.core.remoting.impl.invm.InVMAcceptorFactory";

    static final String queueAddress = "example";

    HornetQServer server;
    ClientSession coreSession;

    @Before
    public void before() throws Exception {
        //Create the Configuration, and set the properties accordingly
        final Configuration configuration = new ConfigurationImpl();
        configuration.setPersistenceEnabled(false);
        configuration.setSecurityEnabled(false);
        configuration.getAcceptorConfigurations().add(new TransportConfiguration(acceptorFactoryName));

        //Create and start the server
        server = HornetQServers.newHornetQServer(configuration);
        server.start();

        final TransportConfiguration transportConfiguration = new TransportConfiguration(connectorFactoryName);
        MessagingResourceProducer.serverLocator = HornetQClient.createServerLocatorWithoutHA(transportConfiguration);
        MessagingResourceProducer.serverLocator.setBlockOnAcknowledge(false);
        MessagingResourceProducer.serverLocator.setConfirmationWindowSize(5);

        MessagingResourceProducer.sessionFactory = MessagingResourceProducer.serverLocator.createSessionFactory();
        coreSession = MessagingResourceProducer.sessionFactory.createSession(false, false, false);
        coreSession.createQueue(queueAddress, queueAddress);
    }

    @After
    public void after() throws Exception {
        if (coreSession != null) {
            coreSession.close();
        }
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void readIBMStockTradeCsvWriteHornetQBeanType() throws Exception {
        testWrite0(writerTestJobName, StockTrade.class, ExcelWriterTest.ibmStockTradeHeader, ExcelWriterTest.ibmStockTradeCellProcessors,
                "1", "10");

        // CsvItemReaderWriter uses header "Date, Time, Open, ..."
        // CsvItemReaderWriter has nameMapping "date, time, open, ..." to match java fields in StockTrade. CsvItemReaderWriter
        // does not understand Jackson mapping annotations in POJO.

        testRead0(readerTestJobName, StockTrade.class, "readIBMStockTradeCsvWriteHornetQBeanType.out",
                ExcelWriterTest.ibmStockTradeNameMapping, ExcelWriterTest.ibmStockTradeHeader,
                ibmStockTradeExpected1_10, ibmStockTradeForbid1_10);
    }

    @Test
    public void readIBMStockTradeCsvWriteHornetQMapType() throws Exception {
        testWrite0(writerTestJobName, Map.class, ExcelWriterTest.ibmStockTradeHeader, ibmStockTradeCellProcessorsDateAsString,
                "1", "10");

        testRead0(readerTestJobName, Map.class, "readIBMStockTradeCsvWriteHornetQMapType.out",
                ExcelWriterTest.ibmStockTradeNameMapping, ExcelWriterTest.ibmStockTradeHeader,
                ibmStockTradeExpected1_10, ibmStockTradeForbid1_10);
    }

    @Test
    public void readIBMStockTradeCsvWriteHornetQListType() throws Exception {
        testWrite0(writerTestJobName, List.class, ExcelWriterTest.ibmStockTradeHeader, ExcelWriterTest.ibmStockTradeCellProcessors,
                "1", "10");

        testRead0(readerTestJobName, List.class, "readIBMStockTradeCsvWriteHornetQListType.out",
                ExcelWriterTest.ibmStockTradeNameMapping, ExcelWriterTest.ibmStockTradeHeader,
                ibmStockTradeExpected1_10, ibmStockTradeForbid1_10);
    }

    public static class HornetQSendAcknowledgementHandler implements SendAcknowledgementHandler {
        @Override
        public void sendAcknowledged(final Message message) {
            System.out.printf("sendAcknowledged message: %s in %s%n", message, this);
        }
    }
}
