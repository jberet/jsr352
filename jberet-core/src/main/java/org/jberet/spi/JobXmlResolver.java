/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jberet.spi;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

/**
 * An interface used to resolve job XML content.
 *
 * <p>
 * Both the {@link #getJobXmlNames(ClassLoader)} and {@link #resolveJobName(String, ClassLoader)} methods are optional.
 * The intention is implementations can use the values returned to query information about specific the job XML files.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public interface JobXmlResolver {

    /**
     * The default {@code META-INF/batch-jobs/} path.
     */
    public final static String DEFAULT_PATH = "META-INF/batch-jobs/";

    /**
     * Locates the job XML and creates a stream to the contents.
     *
     * @param jobXml      the name of the job XML with a {@code .xml} suffix
     * @param classLoader the class loader for the application
     *
     * @return a stream of the job XML or {@code null} if the job XML content was not found
     *
     * @throws java.io.IOException if an error occurs creating the stream
     */
    InputStream resolveJobXml(String jobXml, ClassLoader classLoader) throws IOException;

    /**
     * Optionally returns a list of job XML names that are allowed to be used.
     *
     * <p>
     * An empty collection should be returned if job names can not be resolved.
     * </p>
     *
     * @param classLoader the class loader for the application
     *
     * @return the job XML names or an empty collection
     */
    Collection<String> getJobXmlNames(final ClassLoader classLoader);

    /**
     * Optionally resolves the job name from the job XML.
     *
     * <p>
     * A {@code null} value can be returned if the name cannot be resolved.
     * </p>
     *
     * @param jobXml      the name of the xml XML with a {@code .xml} suffix
     * @param classLoader the class loader for the application
     *
     * @return the name of the job if found or {@code null} if not found
     */
    String resolveJobName(String jobXml, ClassLoader classLoader);
}
