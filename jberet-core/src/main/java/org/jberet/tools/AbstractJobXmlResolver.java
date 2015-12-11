/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jberet.tools;

import java.util.Collection;
import java.util.Collections;

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
}
