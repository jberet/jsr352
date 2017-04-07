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

import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemWriter;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import org.jberet.support._private.SupportLogger;
import org.jberet.support._private.SupportMessages;
import org.jboss.logging.Logger;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemWriter} for easy
 * testing and prototyping. It can be configured to write to console, file,
 * or class field.
 *
 * @since 1.3.0.Beta6
 */
@Named
@Dependent
public class MockItemWriter extends ItemReaderWriterBase implements ItemWriter {

    /**
     * A flag to control whether to write data items to the console.
     * Optional property, and defaults to null.  Valid values are
     * {@code true}, {@code false}, or {@code null}.
     */
    @Inject
    @BatchProperty
    protected Boolean toConsole;

    /**
     * The fully-qualified name of a class that contains a
     * {@code public static java.util.List} field to hold data items.
     * Optional property, and defaults to null.
     */
    @Inject
    @BatchProperty
    protected Class toClass;

    /**
     * The {@code List} field in {@link #toClass} class to save data items.
     */
    protected List listField;


    /**
     * The file path to write data to. Optional property, and defaults to null.
     */
    @Inject
    @BatchProperty
    protected String toFile;

    /**
     * Instructs this class, when the target file already exists, whether to append to, or overwrite
     * the existing resource, or fail.
     * Valid values are {@code append}, {@code overwrite}, and {@code failIfExists}.
     * Optional property, and defaults to {@code append}.
     * This property is used only when {@link #toFile} is specified.
     */
    @Inject
    @BatchProperty
    protected String writeMode;

    /**
     * The {@code PrintWriter} writing to the file specified by {@link #toFile} property.
     */
    protected PrintWriter printWriter;

    /**
     * {@inheritDoc}
     */
    @Override
    public void open(final Serializable checkpoint) throws Exception {
        if (toClass != null) {
            // write to the public static List field of that class
            for (final Field f : toClass.getFields()) {
                if (java.util.List.class.isAssignableFrom(f.getType())) {
                    listField = (List) f.get(null);
                    if (listField == null) {
                        f.set(null, listField = new ArrayList());
                    }
                    break;
                } else {
                    throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, toClass.getName(), "toClass");
                }
            }
        }
        if (toFile != null) {
            resource = toFile;
            printWriter = new PrintWriter(getOutputStream(writeMode), true);
        }
    }

    /**
     * {@inheritDoc}
     * <ul>
     * <li>If {@link #toConsole} is {@code true}, {@code items} are written to the console.
     * <li>If {@link #toConsole} is {@code false}, {@code items} are not written to the console.
     * <li>If {@link #toFile} is specified, {@code items} are written to the specified file.
     * <li>If {@link #toClass} is specified, {@code items} are saved to a public static java.util.List field of {@link #toClass} class.
     *     If the field is not initialized, it will be initialized to a new {@code java.util.ArrayList}.
     * <li>If none of the above property is specified, {@code items} are written to the console.
     * </ul>
     */
    @Override
    public void writeItems(final List<Object> items) throws Exception {
        if (listField != null) {
            listField.addAll(items);
        }

        if (printWriter != null) {
            for (final Object e : items) {
                printWriter.println(e.toString());
            }
        }

        if ((toClass == null && toFile == null && toConsole == null) ||
                toConsole == Boolean.TRUE) {
            for (final Object e : items) {
                System.out.println(e.toString());
            }
            System.out.flush();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws Exception {
        if (printWriter != null) {
            try {
                printWriter.close();
            } catch (Exception e) {
                SupportLogger.LOGGER.logf(Logger.Level.TRACE, e,
                        "Failed to close PrintWriter %s for file %s%n", printWriter, toFile);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Serializable checkpointInfo() throws Exception {
        return null;
    }
}
