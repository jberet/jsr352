/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemReader;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemReader} that reads data items from Kafka topics.
 *
 * @since 1.3.0
 */
@Named
@Dependent
public class KafkaItemReader implements ItemReader {
    /**
     * The file path or URL to the Kafka configuration properties file. See Kafka docs for valid keys and values.
     *
     * @see "org.apache.kafka.clients.consumer.ConsumerConfig"
     */
    @Inject
    @BatchProperty
    protected String configFile;

    /**
     * A list of topic-and-partition in the form of "topicName1:partitionNumber1, topicName2:partitionNumber2, ".
     * For example, "orders:0, orders:1, returns:0, returns:1".
     *
     * @see "org.apache.kafka.common.TopicPartition"
     */
    @Inject
    @BatchProperty
    protected List<String> topicPartitions;

    /**
     * The time, in milliseconds, spent waiting in poll if data is not available. If 0, returns
     * immediately with any records that are available now. Must not be negative.
     *
     * @see "org.apache.kafka.clients.consumer.KafkaConsumer#poll(long)"
     */
    @Inject
    @BatchProperty
    protected long pollTimeout;

    protected KafkaConsumer consumer;

    protected Iterator<ConsumerRecord> recordsBuffer;

    @Override
    public void open(final Serializable checkpoint) throws Exception {
        consumer = new KafkaConsumer(createConfigProperties());
        consumer.assign(createTopicPartitions());

    }

    @Override
    public Object readItem() throws Exception {
        if (recordsBuffer == null || !recordsBuffer.hasNext()) {
            ConsumerRecords records = consumer.poll(pollTimeout);
            if (records == null || records.isEmpty()) {
                return null;
            }
            recordsBuffer = records.iterator();
        }
        if (recordsBuffer.hasNext()) {
            final ConsumerRecord rec = recordsBuffer.next();
            return rec == null ? null : rec.value();
        }

        return null;
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return null;
    }

    @Override
    public void close() {
        if (consumer != null) {
            consumer.close();
            consumer = null;
        }
    }

    private Properties createConfigProperties() throws IOException {
        final Properties configProps = new Properties();
        if (configFile != null) {
            configProps.load(ItemReaderWriterBase.getInputStream(configFile, false));
        }
        return configProps;
    }

    private List<TopicPartition> createTopicPartitions() {
        final List<TopicPartition> tps = new ArrayList<TopicPartition>();
        if (topicPartitions != null) {
            for (final String e : topicPartitions) {
                final int colonPos = e.lastIndexOf(':');
                if (colonPos > 0) {
                    tps.add(new TopicPartition(e.substring(0, colonPos), Integer.parseInt(e.substring(colonPos + 1))));
                } else if (colonPos < 0) {
                    tps.add(new TopicPartition(e, 0));
                } else {
                    throw new KafkaException(topicPartitions.toString());
                }
            }
        }
        return tps;
    }

}
