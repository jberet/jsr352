/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
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
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemReader;
import javax.inject.Inject;
import javax.inject.Named;

import org.jberet.support._private.SupportLogger;
import org.supercsv.io.ICsvBeanReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.io.ICsvReader;

import static org.jberet.support.io.CsvProperties.BEAN_TYPE_KEY;
import static org.jberet.support.io.CsvProperties.NAME_MAPPING_KEY;
import static org.jberet.support.io.CsvProperties.RESOURCE_KEY;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemReader} that reads from a CSV resource into a user-defined
 * bean, java.util.List&lt;String&gt;, or java.util.Map&lt;String, String&gt;. Data files delimited with characters
 * other than comma (e.g., tab, |) are also supported by configuring {@code preference}, {@code delimiterChar},
 * or {@code quoteChar} properties in job xml.
 */
@Named
public class CsvItemReader extends CsvItemReaderWriterBase implements ItemReader {
    @Inject
    @BatchProperty
    protected int start;

    @Inject
    @BatchProperty
    protected int end;

    protected ICsvReader delegateReader;

    @Override
    public void open(final Serializable checkpoint) {
        /**
         * The row number to start reading.  It may be different from the injected field start. During a restart,
         * we would start reading from where it ended during the last run.
         */
        final int startRowNumber;
        if (checkpoint == null) {
            startRowNumber = this.start;
        } else {
            startRowNumber = (Integer) checkpoint;
        }
        if (startRowNumber < this.start || startRowNumber > this.end) {
            throw SupportLogger.LOGGER.invalidStartPosition(startRowNumber, this.start, this.end);
        }
        if (beanType == null) {
            throw SupportLogger.LOGGER.invalidCsvPreference(null, BEAN_TYPE_KEY);
        } else if (java.util.List.class.isAssignableFrom(beanType)) {
            delegateReader = new FastForwardCsvListReader(getInputReader(), getCsvPreference(), startRowNumber);
        } else if (java.util.Map.class.isAssignableFrom(beanType)) {
            delegateReader = new FastForwardCsvMapReader(getInputReader(), getCsvPreference(), startRowNumber);
        } else {
            delegateReader = new FastForwardCsvBeanReader(getInputReader(), getCsvPreference(), startRowNumber);
        }

        final String[] header;
        try {
            header = delegateReader.getHeader(true);//first line check true
            if (this.nameMapping == null) {
                this.nameMapping = header;
            }
        } catch (final IOException e) {
            throw SupportLogger.LOGGER.failToReadCsvHeader(e, resource);
        }
        if (this.end == 0) {
            this.end = Integer.MAX_VALUE;
        }
        if (nameMapping == null) {
            throw SupportLogger.LOGGER.invalidCsvPreference(null, NAME_MAPPING_KEY);
        }
        this.cellProcessorInstances = getCellProcessors(header);
    }

    @Override
    public void close() throws Exception {
        delegateReader.close();
    }

    @Override
    public Object readItem() throws Exception {
        if (delegateReader.getRowNumber() > this.end) {
            return null;
        }
        final Object result;
        if (delegateReader instanceof org.supercsv.io.ICsvBeanReader) {
            if (cellProcessorInstances.length == 0) {
                result = ((ICsvBeanReader) delegateReader).read(beanType, getNameMapping());
            } else {
                result = ((ICsvBeanReader) delegateReader).read(beanType, getNameMapping(), cellProcessorInstances);
            }
        } else if (delegateReader instanceof ICsvListReader) {
            if (cellProcessorInstances.length == 0) {
                result = ((ICsvListReader) delegateReader).read();
            } else {
                result = ((ICsvListReader) delegateReader).read(cellProcessorInstances);
            }
        } else {
            if (cellProcessorInstances.length == 0) {
                result = ((ICsvMapReader) delegateReader).read(getNameMapping());
            } else {
                result = ((ICsvMapReader) delegateReader).read(getNameMapping(), cellProcessorInstances);
            }
        }
        return result;
    }

    @Override
    public Integer checkpointInfo() throws Exception {
        return delegateReader.getRowNumber();
    }

    /**
     * Gets an instance of {@code java.io.Reader} that represents the CSV resource.
     *
     * @return {@code java.io.Reader} that represents the CSV resource
     */
    protected Reader getInputReader() {
        if (resource == null) {
            throw SupportLogger.LOGGER.invalidCsvPreference(resource, RESOURCE_KEY);
        }
        final UnicodeBOMInputStream bomin;
        try {
            InputStream inputStream;
            try {
                final URL url = new URL(resource);
                inputStream = url.openStream();
            } catch (final MalformedURLException e) {
                SupportLogger.LOGGER.notUrl(e, resource);
                final File file = new File(resource);
                if (file.exists()) {
                    inputStream = new FileInputStream(file);
                } else {
                    SupportLogger.LOGGER.notFile(resource);
                    inputStream = CsvItemReader.class.getClassLoader().getResourceAsStream(resource);
                }
            }
            bomin = new UnicodeBOMInputStream(inputStream);
            bomin.skipBOM();
        } catch (final IOException e) {
            throw SupportLogger.LOGGER.failToOpenStream(e, resource);
        }
        return new InputStreamReader(bomin);
    }
}
