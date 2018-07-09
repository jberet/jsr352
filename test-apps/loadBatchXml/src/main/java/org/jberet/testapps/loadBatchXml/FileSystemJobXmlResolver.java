/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.loadBatchXml;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import org.jberet.spi.JobXmlResolver;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class FileSystemJobXmlResolver implements JobXmlResolver {
    private final static File dir = new File(WildFlySecurityManager.getPropertyPrivileged("jberet.job.path.custom", "."));

    @Override
    public InputStream resolveJobXml(final String jobXml, final ClassLoader classLoader) throws IOException {
        final Collection<File> jobFiles = listFiles();
        for (File file : jobFiles) {
            if (jobXml.endsWith(file.getName())) {
                return new BufferedInputStream(new FileInputStream(file));
            }
        }
        return null;
    }

    @Override
    public Collection<String> getJobXmlNames(final ClassLoader classLoader) {
        return Collections2.transform(listFiles(), new Function<File, String>() {
            @Override
            public String apply(final File file) {
                return file.getName();
            }
        });
    }

    @Override
    public String resolveJobName(final String jobXml, final ClassLoader classLoader) {
        return null;
    }

    private Collection<File> listFiles() {
        final File[] files = dir.listFiles(XmlFileFilter.INSTANCE);
        if (files == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(files);
    }

    private static class XmlFileFilter implements FilenameFilter {
        static final XmlFileFilter INSTANCE = new XmlFileFilter();

        @Override
        public boolean accept(final File dir, final String name) {
            return name.endsWith(".xml");
        }
    }
}
