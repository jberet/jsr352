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
import java.util.Set;
import javax.batch.api.BatchProperty;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.jberet.support._private.SupportLogger;
import org.jberet.support._private.SupportMessages;

import static org.jberet.support.io.CsvProperties.APPEND;
import static org.jberet.support.io.CsvProperties.FAIL_IF_DIRS_NOT_EXIST;
import static org.jberet.support.io.CsvProperties.FAIL_IF_EXISTS;
import static org.jberet.support.io.CsvProperties.OVERWRITE;
import static org.jberet.support.io.CsvProperties.RESOURCE_KEY;
import static org.jberet.support.io.CsvProperties.WRITE_MODE_KEY;

/**
 * The base class for all implementations of {@code javax.batch.api.chunk.ItemReader} and
 * {@code javax.batch.api.chunk.ItemWriter}. It also holds batch artifact properties common to all subclasses.
 *
 * @since   1.0.2
 */
public abstract class ItemReaderWriterBase {
    protected static final String NEW_LINE = System.getProperty("line.separator");

    /**
     * The resource to read from (for batch readers), or write to (for batch writers). Some reader or writer
     * implementations may choose to ignore this property and instead use other properties that are more appropriate.
     *
     */
    @Inject
    @BatchProperty
    protected String resource;

    /**
     * Indicates whether the current batch reader will invoke Bean Validation API to validate the incoming data POJO.
     * Optional property and defaults to false, i.e., the reader will validate data POJO bean where appropriate.
     */
    @Inject
    @BatchProperty
    protected boolean skipBeanValidation;

    boolean skipWritingHeader;

    private static class Holder {
        private static final Validator validator = getValidator0();
    }

    /**
     * Gets a cached {@code javax.validation.Validator}.
     *
     * @return {@code javax.validation.Validator}
     */
    public static Validator getValidator() {
        return Holder.validator;
    }

    /**
     * Performs Bean Validation on the passed {@code object}. If any constraint validation errors are found,
     * {@link javax.validation.ConstraintViolationException} is thrown that includes all violation description.
     *
     * @param object the object to be validated
     */
    public static void validate(final Object object) {
        if (object != null) {
            final Set<ConstraintViolation<Object>> violations = getValidator().validate(object);
            if (violations.size() > 0) {
                final StringBuilder sb = new StringBuilder();
                for (final ConstraintViolation<Object> vio : violations) {
                    sb.append(NEW_LINE).append(vio.getConstraintDescriptor()).append(NEW_LINE).append(NEW_LINE);
                    sb.append(vio.getRootBean()).append(NEW_LINE);
                    sb.append(vio.getLeafBean()).append(NEW_LINE);
                    sb.append(vio.getPropertyPath()).append(NEW_LINE);
                    sb.append(vio.getInvalidValue()).append(NEW_LINE);
                    sb.append(vio.getMessage()).append(NEW_LINE).append(NEW_LINE);
                }
                throw new ConstraintViolationException(sb.toString(), violations);
            }
        }
    }

    private static Validator getValidator0() {
        Validator v;
        try {
            v = InitialContext.doLookup("java:comp/Validator");
        } catch (final NamingException e) {
            final ValidatorFactory vf = Validation.buildDefaultValidatorFactory();
            v = vf.getValidator();
        }
        return v;
    }

    /**
     * Gets an instance of {@code java.io.InputStream} that represents the reader resource.
     *
     * @param inputResource the location of the input resource
     * @param detectBOM     if need to detect byte-order mark (BOM). If true, the {@code InputStream} is wrapped inside
     *                      {@code UnicodeBOMInputStream}
     * @return {@code java.io.InputStream} that represents the reader resource
     */
    protected static InputStream getInputStream(final String inputResource, final boolean detectBOM) {
        if (inputResource == null) {
            throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, null, RESOURCE_KEY);
        }
        InputStream inputStream;
        try {
            try {
                final URL url = new URL(inputResource);
                inputStream = url.openStream();
            } catch (final MalformedURLException e) {
                SupportLogger.LOGGER.tracef("The resource %s is not a URL, %s%n", inputResource, e);
                final File file = new File(inputResource);
                if (file.exists()) {
                    inputStream = new FileInputStream(file);
                } else {
                    SupportLogger.LOGGER.tracef("The resource %s is not a file %n", inputResource);
                    ClassLoader cl = Thread.currentThread().getContextClassLoader();
                    if (cl == null) {
                        cl = ItemReaderWriterBase.class.getClassLoader();
                    }
                    inputStream = cl.getResourceAsStream(inputResource);
                }
            }
            if (detectBOM) {
                final UnicodeBOMInputStream bomin = new UnicodeBOMInputStream(inputStream);
                bomin.skipBOM();
                return bomin;
            }
        } catch (final IOException e) {
            throw SupportMessages.MESSAGES.failToOpenStream(e, inputResource);
        }
        return inputStream;
    }

    protected OutputStream getOutputStream(final String writeMode) {
        if (resource == null) {
            throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, null, RESOURCE_KEY);
        }
        try {
            final File file = new File(resource);
            final boolean exists = file.exists();
            // isDirectory check is done in FileOutputStream constructor, no need to do here
            //if (exists && file.isDirectory()) {
            //    throw SupportLogger.LOGGER.writerResourceIsDirectory(file);
            //}
            if (writeMode == null || writeMode.equalsIgnoreCase(APPEND)) {
                return newFileOutputStream(file, exists, true, false);
            }
            if (writeMode.equalsIgnoreCase(OVERWRITE)) {
                return newFileOutputStream(file, exists, false, false);
            }
            if (writeMode.equalsIgnoreCase(FAIL_IF_EXISTS)) {
                if (exists) {
                    throw SupportMessages.MESSAGES.writerResourceAlreadyExists(resource);
                }
                return newFileOutputStream(file, false, false, false);
            }
            if (writeMode.startsWith(FAIL_IF_DIRS_NOT_EXIST)) {
                // writeMode can be specified as along with overwrite
                // writeMode = "failIfDirsNotExist"
                // writeMode = "failIfDirsNotExist overwrite"
                return newFileOutputStream(file, exists, !writeMode.endsWith(OVERWRITE), true);
            }
            throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, writeMode, WRITE_MODE_KEY);
        } catch (final IOException e) {
            throw SupportMessages.MESSAGES.invalidReaderWriterProperty(e, resource, RESOURCE_KEY);
        }
    }

    /**
     * Creates a new {@code FileOutputStream}, depending on the settings in parameters.
     * If the parent directories of the target {@code file} do not exist, they will be
     * automatically created, unless {@code failIfDirsNotExist} is true.
     *
     * @param file the writer target file
     * @param exists whether the {@code file} exists
     * @param append append mode if true; overwrite mode if false
     * @param failIfDirsNotExist if true and if the parent dirs of {@code file} do not exist, throw exception
     * @return the created {@code FileOutputStream}
     * @throws IOException if exception from file operations
     */
    private FileOutputStream newFileOutputStream(final File file,
                                                 final boolean exists,
                                                 final boolean append,
                                                 final boolean failIfDirsNotExist) throws IOException {
        if (!exists) {
            final File parentFile = file.getParentFile();
            if (parentFile == null) {
                throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, resource, RESOURCE_KEY);
            }
            if (!parentFile.exists()) {
                if (failIfDirsNotExist) {
                    throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, resource, RESOURCE_KEY);
                }
                if (!parentFile.mkdirs()) {
                    throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, resource, RESOURCE_KEY);
                }
            }
        }
        final FileOutputStream fos = new FileOutputStream(file, append);
        if (append && file.length() > 0) {
            skipWritingHeader = true;
            fos.write(NEW_LINE.getBytes());
        }
        return fos;
    }

}
