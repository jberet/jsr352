/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jberet.tools;

import java.io.IOException;
import java.io.InputStream;

import org.jberet.spi.JobXmlResolver;

/**
 * A job XML resolver which resolves job XML files in the {@link #DEFAULT_PATH META-INF/batch-jobs} directory on the
 * class loader.
 *
 * <p>
 * The {@link org.jberet.spi.JobXmlResolver#getJobXmlNames(ClassLoader)} and {@link #resolveJobName(String, ClassLoader)} are not implemented as part of this
 * implementation.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public final class MetaInfBatchJobsJobXmlResolver extends AbstractJobXmlResolver implements JobXmlResolver {

    @Override
    public InputStream resolveJobXml(final String jobXml, final ClassLoader classLoader) throws IOException {
        final String path = DEFAULT_PATH + jobXml;
        return classLoader.getResourceAsStream(path);
    }
}
