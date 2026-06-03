/*
 * Copyright (c) 2015-2017 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.tools;

import java.io.IOException;
import java.io.InputStream;

import org.jberet.spi.JobXmlResolver;

/**
 * A job XML resolver which resolves job XML files in the {@link #DEFAULT_PATH META-INF/batch-jobs} directory on the
 * class loader.
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
        return super.resolveJobXml(DEFAULT_PATH + jobXml, classLoader);
    }
}
