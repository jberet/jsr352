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

public class JBeretEndpoint extends DefaultEndpoint {
    private final String remainingPath;

    public JBeretEndpoint(final String endpointUri,
                          final String remainingPath,
                          final Component component) {
        super(endpointUri, component);
        this.remainingPath = remainingPath;
    }

    @Override
    public Producer createProducer() throws Exception {
        return new JBeretProducer(this);
    }

    @Override
    public Consumer createConsumer(final Processor processor) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public PollingConsumer createPollingConsumer() throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    public String getRemainingPath() {
        return remainingPath;
    }
}
