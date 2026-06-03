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
import java.net.URL;
import java.util.Collection;
import java.util.Collections;

import org.jberet._private.BatchLogger;
import org.jberet.spi.JobXmlResolver;

/**
 * An abstract job XML resolver that returns an {@linkplain java.util.Collections#emptyList() empty list} for the
 * {@linkplain #getJobXmlNames(ClassLoader) job XML names} and {@code null} for the {@linkplain #resolveJobName(String,
 * ClassLoader) job name}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public abstract class AbstractJobXmlResolver implements JobXmlResolver {

    @Override
    public Collection<String> getJobXmlNames(final ClassLoader classLoader) {
        return Collections.emptyList();
    }

    @Override
    public String resolveJobName(final String jobXml, final ClassLoader classLoader) {
        return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method tries to resolve the {@code jobXml} using the {@code classLoader}.
     *
     * @since 1.3.0.Beta5
     */
    @Override
    public InputStream resolveJobXml(final String jobXml, final ClassLoader classLoader) throws IOException {
        final URL url = classLoader.getResource(jobXml);

        if (url == null) {
            return null;
        }

        try {
            final InputStream inputStream = url.openStream();
            BatchLogger.LOGGER.resolvedJobXml(url.toExternalForm());
            return inputStream;
        } catch (IOException e) {
            return null;
        }
    }
}
