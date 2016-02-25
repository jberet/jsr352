/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.rest.entity;

import java.io.Serializable;
import javax.batch.operations.BatchRuntimeException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.base.Throwables;

/**
 * Represents a batch exception, {@code BatchRuntimeException}, which includes
 * exception type (class), message, and stack trace text.
 *
 * @since 1.3.0
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"type", "message", "stackTrace"})
public final class BatchExceptionEntity implements Serializable {
    private static final long serialVersionUID = 810435611118287431L;

    @XmlElement
    private final Class<? extends BatchRuntimeException> type;

    @XmlElement
    private final String message;

    @XmlElement
    private final String stackTrace;

    public BatchExceptionEntity(final BatchRuntimeException ex) {
        type = ex.getClass();
        message = ex.getMessage();
        stackTrace = Throwables.getStackTraceAsString(ex);
    }

    public Class<? extends BatchRuntimeException> getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getStackTrace() {
        return stackTrace;
    }
}
