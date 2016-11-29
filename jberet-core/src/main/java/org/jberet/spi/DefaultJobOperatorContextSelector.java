/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jberet.spi;

import org.jberet.operations.JobOperatorImpl;

/**
 * A default context selector.
 * <p>
 * This will require that a {@link BatchEnvironment} service file is available on the class path. This may not work
 * well in a Java EE environment, but should work fine in a Java SE environment.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@SuppressWarnings("WeakerAccess")
public class DefaultJobOperatorContextSelector implements JobOperatorContextSelector {
    private final JobOperatorContext jobOperatorContext;

    /**
     * Creates a new default context selector
     */
    public DefaultJobOperatorContextSelector() {
        jobOperatorContext = JobOperatorContext.create(new JobOperatorImpl());
    }

    @Override
    public JobOperatorContext getJobOperatorContext() {
        return jobOperatorContext;
    }
}
