/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */
 
package org.jberet.support.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;

import org.jberet.support._private.SupportLogger;

import static org.jberet.support.io.CsvProperties.RESOURCE_KEY;

final class ReaderWriterUtil {
    private ReaderWriterUtil() {
    }

    /**
     * Gets an instance of {@code java.io.Reader} that represents the CSV resource.
     *
     * @param detectBOM if need to detect byte-order mark (BOM). If true, the {@code InputStream} is wrapped inside
     *                  {@code UnicodeBOMInputStream}
     * @return {@code java.io.Reader} that represents the CSV resource
     */
    static Reader getInputReader(final String resource, final boolean detectBOM) {
        if (resource == null) {
            throw SupportLogger.LOGGER.invalidCsvPreference(resource, RESOURCE_KEY);
        }
        final InputStreamReader result;
        try {
            InputStream inputStream;
            try {
                final URL url = new URL(resource);
                inputStream = url.openStream();
            } catch (final MalformedURLException e) {
                SupportLogger.LOGGER.tracef("The resource %s is not a URL, %s%n", resource, e);
                final File file = new File(resource);
                if (file.exists()) {
                    inputStream = new FileInputStream(file);
                } else {
                    SupportLogger.LOGGER.tracef("The resource %s is not a file %n", resource);
                    inputStream = CsvItemReader.class.getClassLoader().getResourceAsStream(resource);
                }
            }
            if (detectBOM) {
                final UnicodeBOMInputStream bomin = new UnicodeBOMInputStream(inputStream);
                bomin.skipBOM();
                result = new InputStreamReader(bomin);
            } else {
                result = new InputStreamReader(inputStream);
            }
        } catch (final IOException e) {
            throw SupportLogger.LOGGER.failToOpenStream(e, resource);
        }
        return result;
    }
}
