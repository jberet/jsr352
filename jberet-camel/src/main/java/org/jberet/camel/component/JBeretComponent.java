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

import java.util.Map;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;

/**
 * Camel component class defining JBeret component.
 *
 * @see JBeretEndpoint
 * @see JBeretProducer
 * @since 1.3.0
 */
public class JBeretComponent extends DefaultComponent {
    private final JobOperator jobOperator;

    public JBeretComponent() {
        jobOperator = BatchRuntime.getJobOperator();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method creates an instance of {@link JBeretEndpoint}.
     *
     * @param uri the full URI of the endpoint
     * @param remainingPath the remaining part of the URI without the query
     *                      parameters or component prefix
     * @param parameters the optional parameters passed in
     *
     * @return a newly created {@code JBeretEndpoint} or null if the endpoint cannot be
     *         created based on the inputs
     * @throws Exception is thrown if error creating the endpoint
     */
    @Override
    protected Endpoint createEndpoint(final String uri,
                                      final String remainingPath,
                                      final Map<String, Object> parameters) throws Exception {
        return new JBeretEndpoint(uri, this, remainingPath);
    }

    /**
     * Gets the batch job operator.
     *
     * @return batch {@code JobOperator}
     */
    public JobOperator getJobOperator() {
        return jobOperator;
    }

}
