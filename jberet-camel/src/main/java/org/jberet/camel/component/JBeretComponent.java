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

public class JBeretComponent extends DefaultComponent {
    private final JobOperator jobOperator;

    public JBeretComponent() {
        jobOperator = BatchRuntime.getJobOperator();
    }

    @Override
    protected Endpoint createEndpoint(final String uri,
                                      final String remainingPath,
                                      final Map<String, Object> parameters) throws Exception {
        return new JBeretEndpoint(uri, this, remainingPath, parameters);
    }

    public JobOperator getJobOperator() {
        return jobOperator;
    }


}
