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

package org.jberet.camel.component;

import org.apache.camel.Component;
import org.apache.camel.Consumer;
import org.apache.camel.PollingConsumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;

/**
 * Camel endpoint class defining JBeret endpoint.
 * Query parameters in the URI are all optional, and are applicable
 * in certain operations. All such valid query parameters are
 * automatically bound to the corresponding fields in this class.
 * Any unrecognized query parameters will be rejected with exception.
 * <p>
 * An example of URI with query parameters:
 * <p>
 * {@code jberet:jobinstances?jobName=job1&start=0&count=10}
 *
 * @see JBeretComponent
 * @see JBeretProducer
 * @since 1.3.0
 */
public class JBeretEndpoint extends DefaultEndpoint {
    /**
     * The remaining path in the uri when passing from {@link JBeretComponent},
     * without the scheme part. For instance, {@code jobs/job1/start}.
     */
    private final String remainingPath;

    /**
     * Query parameter named {@code jobName} in the uri, whose value is automatically
     * bound to this field. Default value is {@code null}.
     */
    private String jobName;

    /**
     * Query parameter named  {@code start} in the uri, whose value is automatically
     * bound to this field. Default value is {@code 0}.
     */
    private int start;

    /**
     * Query parameter named {@code count} in the uri, whose value is automatically
     * bound to this field. Default value is {@code 10}.
     */
    private int count = 10;

    /**
     * Instantiates {@code JBeretEndpoint}.
     *
     * @param endpointUri JBeret endpoint uri
     * @param component instance of {@code JBeretComponent}
     * @param remainingPath the remaining path of JBeret endpoint uri
     */
    public JBeretEndpoint(final String endpointUri,
                          final Component component,
                          final String remainingPath) {
        super(endpointUri, component);
        this.remainingPath = remainingPath;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method creates an instance of {@link JBeretProducer}.
     *
     * @return an instance of {@code JBeretProducer}
     * @throws Exception
     */
    @Override
    public Producer createProducer() throws Exception {
        return new JBeretProducer(this);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method always throws {@code UnsupportedOperationException}, as this
     * operation is not supported.
     *
     * @return N/A
     * @throws UnsupportedOperationException
     */
    @Override
    public Consumer createConsumer(final Processor processor) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method always throws {@code UnsupportedOperationException}, as this
     * operation is not supported.
     *
     * @return N/A
     * @throws UnsupportedOperationException
     */
    @Override
    public PollingConsumer createPollingConsumer() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method returns {@code false}, as this endpoint is not a singleton.
     *
     * @return {@code false}
     */
    @Override
    public boolean isSingleton() {
        return false;
    }

    /**
     * Gets the remaining path in the uri when passing from {@link JBeretComponent}.
     *
     * @return the uri without the scheme part. For instance, {@code jobs/job1/start}
     */
    public String getRemainingPath() {
        return remainingPath;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method turns on lenient properties.
     *
     * @return {@code true}
     */
    @Override
    public boolean isLenientProperties() {
        return true;
    }

    /**
     * Gets the value of "jobName" query parameter in the JBeret endpoint URI,
     * which was bound to this class.
     *
     * @return the value of "jobName" query parameter
     */
    public String getJobName() {
        return jobName;
    }

    public void setJobName(final String jobName) {
        this.jobName = jobName;
    }

    /**
     * Gets the value of "start" query parameter in the JBeret endpoint URI,
     * which was bound to this class.
     *
     * @return the value of "start" query parameter
     */
    public int getStart() {
        return start;
    }

    public void setStart(final int start) {
        this.start = start;
    }

    /**
     * Gets the value of "count" query parameter in the JBeret endpoint URI,
     * which was bound to this class.
     *
     * @return the value of "count" query parameter
     */
    public int getCount() {
        return count;
    }

    public void setCount(final int count) {
        this.count = count;
    }
}
