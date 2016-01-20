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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemReader;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.jberet.support._private.SupportMessages;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemReader} that reads data items from Kafka topics.
 *
 * @since 1.3.0
 */
@Named
@Dependent
public class KafkaItemReader extends KafkaItemReaderWriterBase implements ItemReader {
    /**
     * A list of topic-and-partition in the form of "topicName1:partitionNumber1, topicName2:partitionNumber2, ".
     * For example, "orders:0, orders:1, returns:0, returns:1".
     *
     * @see org.jberet.support.io.KafkaItemReaderWriterBase#topicPartitionDelimiter
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

    /**
     * A mapping of topic-partition and its offset to track the progress of each
     * {@code TopicPartition}. The mapping key is of the form "&lt;topicName&gt;:&lt;partitionNumber&gt;, and the value
     * is the offset of that {@code TopicPartition} as a {@code Long} number.
     * This field serves as this item reader's checkpoint data, and must be serializable.
     */
    protected HashMap<String, Long> topicPartitionOffset = new HashMap<String, Long>();

    @SuppressWarnings("unchecked")
    @Override
    public void open(final Serializable checkpoint) throws Exception {
        consumer = new KafkaConsumer(createConfigProperties());
        consumer.assign(createTopicPartitions());

        if (checkpoint != null) {
            final HashMap<String, Long> chkp = (HashMap<String, Long>) checkpoint;
            for (final Map.Entry<String, Long> e : chkp.entrySet()) {
                final String key = e.getKey();
                final String topic;
                final int partition;
                final int colonPos = key.lastIndexOf(topicPartitionDelimiter);
                if (colonPos > 0) {
                    topic = key.substring(0, colonPos);
                    partition = Integer.parseInt(key.substring(colonPos + 1));
                } else if (colonPos < 0) {
                    topic = key;
                    partition = 0;
                } else {
                    throw SupportMessages.MESSAGES.invalidCheckpoint(checkpoint);
                }
                final long newStartPosition = chkp.get(key) + 1;
                consumer.seek(new TopicPartition(topic, partition), newStartPosition);
            }
        }
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return topicPartitionOffset;
    }

    @SuppressWarnings("unchecked")
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
            if (rec == null) {
                return null;
            }
            final Object val = rec.value();
            topicPartitionOffset.put(rec.topic() + topicPartitionDelimiter + rec.partition(), rec.offset());
            return val;
        }
        return null;
    }

    @Override
    public void close() {
        if (consumer != null) {
            consumer.close();
            consumer = null;
        }
    }

    protected List<TopicPartition> createTopicPartitions() {
        final List<TopicPartition> tps = new ArrayList<TopicPartition>();
        if (topicPartitions != null) {
            for (final String e : topicPartitions) {
                final int colonPos = e.lastIndexOf(topicPartitionDelimiter);
                if (colonPos > 0) {
                    tps.add(new TopicPartition(e.substring(0, colonPos), Integer.parseInt(e.substring(colonPos + 1))));
                } else if (colonPos < 0) {
                    tps.add(new TopicPartition(e, 0));
                } else {
                    throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, topicPartitions.toString(), "topicPartitions");
                }
            }
        } else {
            throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, null, "topicPartitions");
        }
        return tps;
    }
}
