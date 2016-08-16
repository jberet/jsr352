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

package org.jberet.camel;

import java.io.Serializable;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemReader;
import javax.inject.Inject;
import javax.inject.Named;

import _private.JBeretCamelLogger;
import org.apache.camel.ConsumerTemplate;

/**
 * Implementation of {@code javax.batch.api.chunk.ItemReader} that reads batch data
 * from Apache Camel endpoint. The source Camel endpoint is configured through batch property
 * {@code endpoint} in job XML. For each read operation, this reader will wait up to
 * the configured {@code timeout} milliseconds for data. Users may also configure
 * {@code beanType} batch property to specify the expected Java type of the
 * data.
 * <p>
 * An example job.xml using {@code camelItemReader}:
 * <pre>
 * &lt;job id="camelReaderTest" xmlns="http://xmlns.jcp.org/xml/ns/javaee" version="1.0"&gt;
 *   &lt;step id="camelReaderTest.step1"&gt;
 *     &lt;chunk&gt;
 *       &lt;reader ref="camelItemReader"&gt;
 *         &lt;properties&gt;
 *           &lt;property name="beanType" value="org.jberet.samples.wildfly.common.Movie"/&gt;
 *           &lt;property name="endpoint" value="#{jobParameters['endpoint']}"/&gt;
 *           &lt;property name="timeout" value="#{jobParameters['timeout']}"/&gt;
 *         &lt;/properties&gt;
 *       &lt;/reader&gt;
 * </pre>
 *
 * @see CamelItemWriter
 * @see CamelItemProcessor
 * @since 1.3.0
 */
@Named
public class CamelItemReader extends CamelArtifactBase implements ItemReader {

    /**
     * Timeout in milliseconds for this reader to wait for a response from
     * Camel endpoint. After the timeout expires, {@code null} is returned.
     */
    @Inject
    @BatchProperty
    protected long timeout;

    /**
     * The Java type of the batch data read from Camel endpoint.
     * This is an optional batch property, and if not set,
     * {@code java.lang.Object} is assumed.
     */
    @Inject
    @BatchProperty
    protected Class beanType;

    /**
     * The Camel {@code ConsumerTemplate} for reading data from Camel endpoint.
     */
    protected ConsumerTemplate consumerTemplate;

    /**
     * {@inheritDoc}
     * <p>
     * This method gets resources ready, such as {@link #consumerTemplate}.
     *
     * @param checkpoint the last checkpoint
     * @throws Exception
     */
    @Override
    public void open(final Serializable checkpoint) throws Exception {
        init();
        if (consumerTemplate == null) {
            consumerTemplate = camelContext.createConsumerTemplate();
        }
        JBeretCamelLogger.LOGGER.openReader(this, endpointUri, camelContext, consumerTemplate);
        consumerTemplate.start();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method closes any resources used by this class, such as {@link #consumerTemplate}.
     *
     * @throws Exception
     */
    @Override
    public void close() throws Exception {
        if (consumerTemplate != null) {
            consumerTemplate.stop();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method reads from the configured Camel endpoint.
     *
     * @return next item or {@code null}
     * @throws Exception is thrown for any errors
     */
    @Override
    public Object readItem() throws Exception {
        final Object item;
        if (beanType == null) {
            item = consumerTemplate.receiveBody(endpoint, timeout);
        } else {
            item = consumerTemplate.receiveBody(endpoint, timeout, beanType);
        }
        return item;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method always returns {@code null}.
     *
     * @return {@code null}
     */
    @Override
    public Serializable checkpointInfo() {
        return null;
    }
}
