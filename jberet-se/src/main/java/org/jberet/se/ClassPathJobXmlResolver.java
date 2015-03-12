/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jberet.se;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

import org.jberet.spi.JobXmlResolver;

/**
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class ClassPathJobXmlResolver implements JobXmlResolver {

    @Override
    public InputStream resolveJobXml(final String jobXml, final ClassLoader classLoader) throws IOException {
        return classLoader.getResourceAsStream(jobXml);
    }

    @Override
    public Collection<String> getJobXmlNames(final ClassLoader classLoader) {
        return Collections.emptyList();
    }

    @Override
    public String resolveJobName(final String jobXml, final ClassLoader classLoader) {
        return null;
    }
}
