/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.amqp;

import static org.jberet.testapps.amqp.MessagingResourceProducer.connectionFactory;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.jms.Connection;
import javax.jms.Session;

import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.testapps.common.AbstractIT;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;

@Ignore("This test class needs ActiveMQ Artemis standalone server running in a separate process.")
public class AmqpIT extends AbstractIT {
    private final JobOperator jobOperator = BatchRuntime.getJobOperator();
    static final String jberetTmpDir = System.getProperty("jberet.tmp.dir");
    static final String amqpReaderTestJob = "amqpReaderTest";
    static final String amqpWriterTestJob = "amqpWriterTest";
    static final String queueName = "jms.queue.exampleQueue";

    static final String artemisUrl = "amqp://localhost:5672";

    @BeforeClass
    public static void beforeClass() throws Exception {
        connectionFactory = new JmsConnectionFactory(artemisUrl);
    }

    @Test
    public void amqpWriterReaderTest() throws Exception {
        Connection connection = null;
        try {
            // Step 1. Create an amqp qpid 1.0 connection
            connection = connectionFactory.createConnection();

            // Step 2. Create a session
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // Step 3. Create a sender
            MessagingResourceProducer.queue = session.createQueue(queueName);

            connection.start();
        } finally {
            if (connection != null) {
                connection.close();
            }
        }

        final long writerJobExecutionId = jobOperator.start(amqpWriterTestJob, null);
        System.out.printf("Starting job %s with execution id: %s%n", amqpWriterTestJob, writerJobExecutionId);
        final JobExecutionImpl writerJobExecution = (JobExecutionImpl) jobOperator.getJobExecution(writerJobExecutionId);
        writerJobExecution.awaitTermination(5, TimeUnit.MINUTES);
        System.out.printf("%s finished with batch status %s%n", amqpWriterTestJob, writerJobExecution.getBatchStatus());
        assertEquals(BatchStatus.COMPLETED, writerJobExecution.getBatchStatus());

        final Properties jobParams = new Properties();
        final String outputFilePath = new File(jberetTmpDir, "amqpWriterReaderTest").getAbsolutePath();
        System.out.printf("jberet.tmp.dir: %s%n", jberetTmpDir);
        jobParams.setProperty("writeResource", outputFilePath);

        final long readerJobExecutionId = jobOperator.start(amqpReaderTestJob, jobParams);
        System.out.printf("Starting job %s with execution id: %s%n", amqpReaderTestJob, readerJobExecutionId);
        final JobExecutionImpl readerJobExecution = (JobExecutionImpl) jobOperator.getJobExecution(readerJobExecutionId);
        readerJobExecution.awaitTermination(5, TimeUnit.MINUTES);
        System.out.printf("%s finished with batch status %s%n", amqpReaderTestJob, readerJobExecution.getBatchStatus());
        System.out.printf("Output file: %s%n", outputFilePath);
        assertEquals(BatchStatus.COMPLETED, readerJobExecution.getBatchStatus());
    }
}
