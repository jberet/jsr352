/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jberet.spi.JobXmlResolver;

/**
 * A job XML resolver for chaining resolvers.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class ChainedJobXmlResolver implements JobXmlResolver {
    private final Set<JobXmlResolver> resolvers;

    /**
     * Creates a new chained job XML resolver.
     *
     * @param resolvers the resolvers to use
     */
    public ChainedJobXmlResolver(final JobXmlResolver... resolvers) {
        this.resolvers = new LinkedHashSet<JobXmlResolver>(resolvers.length);
        Collections.addAll(this.resolvers, resolvers);
    }

    /**
     * Creates a new chained job XML resolver.
     *
     * @param resolvers the resolvers to use
     */
    public ChainedJobXmlResolver(final Iterable<JobXmlResolver> resolvers) {
        this.resolvers = new LinkedHashSet<JobXmlResolver>();
        for (JobXmlResolver resolver : resolvers) {
            this.resolvers.add(resolver);
        }
    }

    /**
     * Creates a new chained job XML resolver.
     *
     * @param resolvers           the resolvers to use
     * @param additionalResolvers any additional resolvers to use
     */
    public ChainedJobXmlResolver(final Iterable<JobXmlResolver> resolvers, final JobXmlResolver... additionalResolvers) {
        this.resolvers = new LinkedHashSet<JobXmlResolver>();
        for (JobXmlResolver resolver : resolvers) {
            this.resolvers.add(resolver);
        }
        Collections.addAll(this.resolvers, additionalResolvers);
    }

    /**
     * Creates a new chained job XML resolver.
     *
     * @param resolvers the resolvers to use
     */
    public ChainedJobXmlResolver(final Collection<JobXmlResolver> resolvers) {
        this.resolvers = new LinkedHashSet<JobXmlResolver>(resolvers);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Each resolver is checked until the first one returns a {@code non-null} value after which no other resolvers are
     * checked. If no resolver finds the content {@code null} will be returned.
     * </p>
     */
    @Override
    public InputStream resolveJobXml(final String jobXml, final ClassLoader classLoader) throws IOException {
        for (JobXmlResolver resolver : resolvers) {
            final InputStream is = resolver.resolveJobXml(jobXml, classLoader);
            if (is != null) {
                return is;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Gets the names for each resolver and returns all the names found.
     * </p>
     */
    @Override
    public Collection<String> getJobXmlNames(final ClassLoader classLoader) {
        final Collection<String> names = new ArrayList<String>();
        for (JobXmlResolver resolver : resolvers) {
            names.addAll(resolver.getJobXmlNames(classLoader));
        }
        return names;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Checks each resolver until a {@code non-null} value is found. If the name could not be resolved by any resolver
     * {@code null} is returned.
     * </p>
     */
    @Override
    public String resolveJobName(final String jobXml, final ClassLoader classLoader) {
        for (JobXmlResolver resolver : resolvers) {
            final String name = resolver.resolveJobName(jobXml, classLoader);
            if (name != null) {
                return name;
            }
        }
        return null;
    }
}
