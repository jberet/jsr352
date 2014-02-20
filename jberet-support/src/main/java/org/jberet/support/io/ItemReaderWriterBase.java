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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import javax.batch.api.BatchProperty;
import javax.inject.Inject;

import org.jberet.support._private.SupportLogger;

import static org.jberet.support.io.CsvProperties.APPEND;
import static org.jberet.support.io.CsvProperties.FAIL_IF_EXISTS;
import static org.jberet.support.io.CsvProperties.OVERWRITE;
import static org.jberet.support.io.CsvProperties.RESOURCE_KEY;
import static org.jberet.support.io.CsvProperties.WRITE_MODE_KEY;

/**
 * The base class for all implementations of {@code javax.batch.api.chunk.ItemReader} and
 * {@code javax.batch.api.chunk.ItemWriter}. It also holds batch artifact properties common to all subclasses.
 */
public abstract class ItemReaderWriterBase {
    protected static final String NEW_LINE = System.getProperty("line.separator");

    @Inject
    @BatchProperty
    protected String resource;

    boolean skipWritingHeader;


    /**
     * Gets an instance of {@code java.io.InputStream} that represents the reader resource.
     *
     * @param detectBOM if need to detect byte-order mark (BOM). If true, the {@code InputStream} is wrapped inside
     *                  {@code UnicodeBOMInputStream}
     * @return {@code java.io.InputStream} that represents the reader resource
     */
    protected InputStream getInputStream(final boolean detectBOM) {
        if (resource == null) {
            throw SupportLogger.LOGGER.invalidReaderWriterProperty(null, null, RESOURCE_KEY);
        }
        InputStream inputStream;
        try {
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
                    inputStream = ItemReaderWriterBase.class.getClassLoader().getResourceAsStream(resource);
                }
            }
            if (detectBOM) {
                final UnicodeBOMInputStream bomin = new UnicodeBOMInputStream(inputStream);
                bomin.skipBOM();
                return bomin;
            }
        } catch (final IOException e) {
            throw SupportLogger.LOGGER.failToOpenStream(e, resource);
        }
        return inputStream;
    }

    protected OutputStream getOutputStream(final String writeMode) {
        if (resource == null) {
            throw SupportLogger.LOGGER.invalidReaderWriterProperty(null, null, RESOURCE_KEY);
        }
        try {
            final File file = new File(resource);
            final boolean exists = file.exists();
            if (exists && file.isDirectory()) {
                throw SupportLogger.LOGGER.writerResourceIsDirectory(file);
            }
            if (writeMode == null || writeMode.equalsIgnoreCase(APPEND)) {
                final FileOutputStream fos = new FileOutputStream(file, true);
                if (file.length() > 0) {
                    skipWritingHeader = true;
                    fos.write(NEW_LINE.getBytes());
                }
                return fos;
            }
            if (writeMode.equalsIgnoreCase(OVERWRITE)) {
                return new FileOutputStream(file);
            }
            if (writeMode.equalsIgnoreCase(FAIL_IF_EXISTS)) {
                if (exists) {
                    throw SupportLogger.LOGGER.writerResourceAlreadyExists(file.getPath());
                }
                return new FileOutputStream(file);
            }
            throw SupportLogger.LOGGER.invalidReaderWriterProperty(null, writeMode, WRITE_MODE_KEY);
        } catch (final IOException e) {
            throw SupportLogger.LOGGER.invalidReaderWriterProperty(e, resource, RESOURCE_KEY);
        }
    }

}
