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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import javax.batch.api.BatchProperty;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

import org.jberet.support._private.SupportLogger;

import static org.jberet.support.io.CsvProperties.APPEND;
import static org.jberet.support.io.CsvProperties.FAIL_IF_EXISTS;
import static org.jberet.support.io.CsvProperties.OVERWRITE;
import static org.jberet.support.io.CsvProperties.RESOURCE_KEY;
import static org.jberet.support.io.CsvProperties.RESOURCE_STEP_CONTEXT;
import static org.jberet.support.io.CsvProperties.WRITE_MODE_KEY;

public abstract class ItemReaderWriterBase {
    @Inject
    @BatchProperty
    protected String resource;

    boolean skipWritingHeader;
    StringWriter stringWriter;

    /**
     * Gets an instance of {@code java.io.Reader} that represents the CSV resource.
     *
     * @param detectBOM if need to detect byte-order mark (BOM). If true, the {@code InputStream} is wrapped inside
     *                  {@code UnicodeBOMInputStream}
     * @return {@code java.io.Reader} that represents the CSV resource
     */
    protected Reader getInputReader(final boolean detectBOM) {
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

    protected Writer getOutputWriter(final String writeMode, final StepContext stepContext) {
        if (resource == null) {
            throw SupportLogger.LOGGER.invalidCsvPreference(resource, RESOURCE_KEY);
        }
        if (resource.equalsIgnoreCase(RESOURCE_STEP_CONTEXT)) {
            if (OVERWRITE.equalsIgnoreCase(writeMode)) {
                return stringWriter = new StringWriter();
            }
            final Object transientUserData = stepContext.getTransientUserData();
            if (writeMode == null || writeMode.equalsIgnoreCase(APPEND)) {
                if (transientUserData != null) {
                    if (transientUserData instanceof String) {
                        skipWritingHeader = true;
                    } else {
                        throw SupportLogger.LOGGER.cannotAppendToNonStringData(transientUserData.getClass());
                    }
                }
                return stringWriter = new StringWriter();
            }
            if (writeMode.equalsIgnoreCase(FAIL_IF_EXISTS)) {
                if (transientUserData != null) {
                    throw SupportLogger.LOGGER.csvResourceAlreadyExists(transientUserData);
                }
                return stringWriter = new StringWriter();
            }
            throw SupportLogger.LOGGER.invalidCsvPreference(writeMode, WRITE_MODE_KEY);
        }
        try {
            final File file = new File(resource);
            final boolean exists = file.exists();
            if (exists && file.isDirectory()) {
                throw SupportLogger.LOGGER.csvResourceIsDirectory(file);
            }
            if (writeMode == null || writeMode.equalsIgnoreCase(APPEND)) {
                if (file.length() > 0) {
                    skipWritingHeader = true;
                }
                return new FileWriter(file, true);
            }
            if (writeMode.equalsIgnoreCase(OVERWRITE)) {
                return new FileWriter(file);
            }
            if (writeMode.equalsIgnoreCase(FAIL_IF_EXISTS)) {
                if (exists) {
                    throw SupportLogger.LOGGER.csvResourceAlreadyExists(file.getPath());
                }
                return new FileWriter(resource);
            }
            throw SupportLogger.LOGGER.invalidCsvPreference(writeMode, WRITE_MODE_KEY);
        } catch (final IOException e) {
            throw SupportLogger.LOGGER.invalidCsvPreference(resource, RESOURCE_KEY);
        }
    }

}
