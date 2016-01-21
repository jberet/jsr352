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
 * An implementation of {@code ItemReader} that reads data items from Kafka topics.
 * This reader class keeps track of the current read position, including current topic name, topic partition number,
 * and topic partition offset. Therefore, it is recommended to disable Kafka auto commit in Kafka consumer properties,
 * e.g., {@code enable.auto.commit=false}. Kafka consumer properties are specified in batch property {@link #configFile}.
 * <p>
 * This reader class supports retry and restart, using the tracked read position as checkpoint info.
 * <p>
 * It is also recommended to turn off Kafka consumer automatic group management; instead manually assign topics and
 * partitions for the consumer. See batch property {@link #topicPartitions}.
 *
 * @see KafkaItemWriter
 * @see KafkaItemReaderWriterBase
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

    /**
     * Kafka consumer instance based on configuration properties specified in {@link #configFile}.
     * It is created in {@link #open(Serializable)} method, and closed in {@link #close()} method.
     *
     */
    protected KafkaConsumer consumer;

    /**
     * Holds records obtained from polling Kafka server, and feeds to {@link #readItem()} method one record a time.
     * When it is null, or contains no more element, {@link #readItem()} method polls Kafka server to obtain more
     * records.
     */
    protected Iterator<ConsumerRecord> recordsBuffer;

    /**
     * A mapping of topic-partition and its offset to track the progress of each
     * {@code TopicPartition}. The mapping key is of the form "&lt;topicName&gt;:&lt;partitionNumber&gt;, and the value
     * is the offset of that {@code TopicPartition} as a {@code Long} number.
     * This field serves as this item reader's checkpoint data, and must be serializable.
     */
    protected HashMap<String, Long> topicPartitionOffset = new HashMap<String, Long>();

    /**
     * During the reader opening, the Kafka consumer is instantiated, and {@code checkpoint}, if any, is analyzed to
     * position the reader properly. The Kafka consumer is created based on the configuration properties as specified
     * in the batch property {@link #configFile}. The consumer is then assigned topic partitions as specified in the
     * batch property {@link #topicPartitions}.
     *
     * @param checkpoint checkpoint info, null for the first invocation in a new job execution
     * @throws Exception if error occurs
     */
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

    /**
     * Returns reader checkpoint info that includes topic name, partition number and partition offset for each
     * topic partition assigned to current Kafka consumer.
     *
     * @return reader checkpoint info as {@code HashMap<String, Long>}
     */
    @Override
    public Serializable checkpointInfo() {
        return topicPartitionOffset;
    }

    /**
     * Reads 1 record and return its value object, and updates the current read position.
     * Since Kafka consumer poll operation retrieves a collection of records, which are cached in this reader class to
     * mimic the read-one-item-at-a-time behavior. Therefore, Kafka consumer poll operation is only invoked when the
     * local cache does not exist or contains no more entry. If no more record can be retrieved from Kafka server, null
     * is returned.
     *
     * @return the value object of the read record from Kafka server
     *
     * @throws Exception if error occurs
     */
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

    /**
     * Closes the Kafka consumer.
     */
    @Override
    public void close() {
        if (consumer != null) {
            consumer.close();
            consumer = null;
        }
    }

    /**
     * Creates and returns a list of {@code TopicPartition} based on the injected batch property {@link #topicPartitions}.
     *
     * @return a list of {@code org.apache.kafka.common.TopicPartition}
     */
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
