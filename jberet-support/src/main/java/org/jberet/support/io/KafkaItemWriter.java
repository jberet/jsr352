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
import java.util.List;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemWriter;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.jberet.support._private.SupportMessages;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemWriter} that sends data items to Kafka topics.
 *
 * @see KafkaItemReader
 * @since 1.3.0
 */
@Named
@Dependent
public class KafkaItemWriter extends KafkaItemReaderWriterBase implements ItemWriter {
    /**
     * A topic partition in the form of "topicName:partitionNumber".
     * For example, "orders:0".
     *
     * @see KafkaItemReaderWriterBase#topicPartitionDelimiter
     * @see "org.apache.kafka.common.TopicPartition"
     */
    @Inject
    @BatchProperty
    protected String topicPartition;

    /**
     * The key used when sending {@code ProducerRecord}.
     *
     * @see "org.apache.kafka.clients.producer.ProducerRecord"
     */
    @Inject
    @BatchProperty
    protected String recordKey;

    protected KafkaProducer producer;
    private String topic;
    private Integer partition;

    @Override
    public void open(final Serializable checkpoint) throws Exception {
        producer = new KafkaProducer(createConfigProperties());

        if (topicPartition == null) {
            throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, null, "topicPartition");
        }
        final int colonPos = topicPartition.lastIndexOf(topicPartitionDelimiter);
        if (colonPos > 0) {
            topic = topicPartition.substring(0, colonPos);
            partition = Integer.valueOf(topicPartition.substring(colonPos + 1));
        } else if (colonPos < 0) {
            topic = topicPartition;
        } else {
            throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, topicPartition, "topicPartition");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void writeItems(final List<Object> items) throws Exception {
        for (final Object item : items) {
            producer.send(new ProducerRecord(getTopic(item), getPartition(item), getRecordKey(item), item));
        }
    }

    @Override
    public void close() {
        if (producer != null) {
            producer.close();
            producer = null;
        }
    }

    /**
     * Gets the destination topic used when sending {@code ProducerRecord}.
     * Subclass may override this method to provide a suitable topic.
     * The default implementation returns a value based on the injected field {@link #topicPartition}.
     *
     * @param item the item currently being sent
     *
     * @return topic used for sending the current {@code ProducerRecord}
     */
    @SuppressWarnings("unused")
    protected String getTopic(final Object item) {
        return topic;
    }

    /**
     * Gets the destination topic partition used when sending {@code ProducerRecord}.
     * Subclass may override this method to provide a suitable topic partition number.
     * The default implementation returns a value based on the injected field {@link #topicPartition}.
     *
     * @param item the item currently being sent
     *
     * @return topic partition used for sending the current {@code ProducerRecord}
     */
    @SuppressWarnings("unused")
    protected Integer getPartition(final Object item) {
        return partition;
    }

    /**
     * Gets the key used when sending {@code ProducerRecord}.
     * Subclass may override this method to provide a suitable key.
     * The default implementation returns the injected value of field {@link #recordKey}.
     *
     * @param item the item currently being sent
     *
     * @return a key used for sending the current {@code ProducerRecord}
     */
    @SuppressWarnings("unused")
    protected String getRecordKey(final Object item) {
        return recordKey;
    }
}
