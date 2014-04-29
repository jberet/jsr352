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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.util.Locale;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemReader;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import org.beanio.BeanReader;
import org.beanio.BeanReaderErrorHandler;
import org.beanio.StreamFactory;
import org.jberet.support._private.SupportLogger;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemReader} based on BeanIO. This reader class handles all
 * data formats that are supported by BeanIO, e.g., fixed length file, CSV file, XML, etc. It supports restart,
 * ranged reading, custom error handler, and dynamic BeanIO mapping properties. {@link org.jberet.support.io.BeanIOItemReader}
 * configurations are specified as reader properties in job xml.
 */
@Named
@Dependent
public class BeanIOItemReader extends BeanIOItemReaderWriterBase implements ItemReader {
    /**
     * A positive integer indicating the start position in the input resource. It is optional and defaults to 1
     * (starting from the 1st data item).
     */
    @Inject
    @BatchProperty
    protected int start;

    /**
     * A positive integer indicating the end position in the input resource. It is optional and defaults to
     * {@code Integer.MAX_VALUE}.
     */
    @Inject
    @BatchProperty
    protected int end;

    /**
     * A class implementing {@link org.beanio.BeanReaderErrorHandler} for handling exceptions thrown by a
     * {@link BeanReader}.
     */
    @Inject
    @BatchProperty
    protected Class errorHandler;

    @Inject
    @BatchProperty
    protected String locale;

    private BeanReader beanReader;
    protected int currentPosition;

    @Override
    public void open(final Serializable checkpoint) throws Exception {
        /**
         * The row number to start reading.  It may be different from the injected field start. During a restart,
         * we would start reading from where it ended during the last run.
         */
        if (this.end == 0) {
            this.end = Integer.MAX_VALUE;
        }
        if (this.start == 0) {
            this.start = 1;
        }
        final int startRowNumber = checkpoint == null ? this.start : (Integer) checkpoint;
        if (startRowNumber < this.start || startRowNumber > this.end || startRowNumber < 0) {
            throw SupportLogger.LOGGER.invalidStartPosition(startRowNumber, this.start, this.end);
        }

        mappingFileKey = new StreamFactoryKey(jobContext, streamMapping);
        final StreamFactory streamFactory = getStreamFactory(streamFactoryLookup, mappingFileKey, mappingProperties);
        final InputStream inputStream = getInputStream(resource, false);
        final Reader inputReader = charset == null ? new InputStreamReader(inputStream) :
                new InputStreamReader(inputStream, charset);
        beanReader = streamFactory.createReader(streamName, new BufferedReader(inputReader), getLocale());

        if (errorHandler != null) {
            beanReader.setErrorHandler((BeanReaderErrorHandler) errorHandler.newInstance());
        }
        if (startRowNumber > 1) {
            beanReader.skip(startRowNumber - 1);
            currentPosition += startRowNumber - 1;
        }
    }

    @Override
    public Object readItem() throws Exception {
        if (++currentPosition > end) {
            return null;
        }
        return beanReader.read();
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return currentPosition;
    }

    @Override
    public void close() throws Exception {
        if (beanReader != null) {
            beanReader.close();
            beanReader = null;
            mappingFileKey = null;
        }
    }

    private Locale getLocale() {
        if (locale == null) {
            return Locale.getDefault();
        }
        final String[] parts = locale.split("_", -1);
        if (parts.length == 1) {
            return new Locale(parts[0]);
        } else if (parts.length == 2 || (parts.length == 3 && parts[2].startsWith("#"))) {
            return new Locale(parts[0], parts[1]);
        }
        return new Locale(parts[0], parts[1], parts[2]);
    }
}
