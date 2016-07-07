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

import java.util.Properties;
import javax.batch.operations.JobOperator;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;

public class JBeretProducer extends DefaultProducer {
    public JBeretProducer(final Endpoint endpoint) {
        super(endpoint);
    }

    @Override
    public void process(final Exchange exchange) throws Exception {
        final JBeretEndpoint endpoint = (JBeretEndpoint) getEndpoint();
        final JBeretComponent component = (JBeretComponent) endpoint.getComponent();
        final JobOperator jobOperator = component.getJobOperator();

        final String remainingPath = endpoint.getRemainingPath();
        final Properties jobParams = (Properties) exchange.getIn().getBody();
        final long jobExecutionId = jobOperator.start(remainingPath, jobParams);
        exchange.getIn().setBody(jobExecutionId, Long.class);

    }
}
