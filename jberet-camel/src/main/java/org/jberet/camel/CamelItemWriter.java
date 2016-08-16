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
import java.util.List;
import javax.batch.api.chunk.ItemWriter;
import javax.enterprise.context.Dependent;
import javax.inject.Named;

import _private.JBeretCamelLogger;
import org.apache.camel.ProducerTemplate;

/**
 * Implementation of {@code javax.batch.api.chunk.ItemWriter} that writes batch data
 * to Apache Camel endpoint.
 * <p>
 * The target Camel endpoint is configured through batch property
 * {@code endpoint} in job XML. For example,
 * <pre>
 * &lt;job id="camelWriterTest" xmlns="http://xmlns.jcp.org/xml/ns/javaee" version="1.0"&gt;
 *   &lt;step id="camelWriterTest.step1"&gt;
 *     &lt;chunk&gt;
 *       ... ...
 *       &lt;writer ref="camelItemWriter"&gt;
 *         &lt;properties&gt;
 *           &lt;property name="endpoint" value="#{jobParameters['endpoint']}"/&gt;
 *         &lt;/properties&gt;
 *       &lt;/writer&gt;
 *     &lt;/chunk&gt;
 *   &lt;/step&gt;
 * &lt;/job&gt;
 * </pre>
 *
 * @see CamelItemReader
 * @see CamelItemProcessor
 * @since 1.3.0
 */
@Named
@Dependent
public class CamelItemWriter extends CamelArtifactBase implements ItemWriter {

    /**
     * The Camel {@code ProducerTemplate} for writing data to the endpoint.
     */
    protected ProducerTemplate producerTemplate;

    /**
     * {@inheritDoc}
     * <p>
     * This method gets resources ready, such as {@link #producerTemplate}.
     *
     * @param checkpoint the last checkpoint
     * @throws Exception if any errors occur
     */
    @Override
    public void open(final Serializable checkpoint) throws Exception {
        init();
        if (producerTemplate == null) {
            producerTemplate = camelContext.createProducerTemplate();
        }
        JBeretCamelLogger.LOGGER.openWriter(this, endpointUri, camelContext, producerTemplate);
        producerTemplate.start();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method closes any resources used by this class, such as {@link #producerTemplate}.
     *
     * @throws Exception
     */
    @Override
    public void close() throws Exception {
        if (producerTemplate != null) {
            producerTemplate.stop();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method writes the data {@code items} to the configured Camel endpoint.
     *
     * @param items the list of items to write
     * @throws Exception if any errors occur
     */
    @Override
    public void writeItems(final List<Object> items) throws Exception {
        for (final Object e : items) {
            producerTemplate.sendBody(endpoint, e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method always returns null.
     *
     * @return {@code null}
     */
    @Override
    public Serializable checkpointInfo() {
        return null;
    }
}
