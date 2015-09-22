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

package org.jberet.rest.model;

import java.io.Serializable;
import javax.batch.operations.BatchRuntimeException;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.Throwables;

@XmlRootElement
public final class BatchRuntimeExceptionData implements Serializable {
    private static final long serialVersionUID = 810435611118287431L;

    private final Class<? extends BatchRuntimeException> type;
    private final String message;
    private final String stackTrace;

    public BatchRuntimeExceptionData(final BatchRuntimeException ex) {
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
