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

package org.jberet.rest.exception;

import javax.batch.operations.BatchRuntimeException;
import javax.batch.operations.JobExecutionAlreadyCompleteException;
import javax.batch.operations.JobExecutionIsRunningException;
import javax.batch.operations.JobExecutionNotMostRecentException;
import javax.batch.operations.JobExecutionNotRunningException;
import javax.batch.operations.JobSecurityException;
import javax.batch.operations.NoSuchJobException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.operations.NoSuchJobInstanceException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.jberet.rest._private.RestAPILogger;
import org.jberet.rest.entity.BatchExceptionEntity;

/**
 * Exception mapper for {@code BatchRuntimeException}, and maps various sub-types of {@code BatchRuntimeException}
 * to appropriate response status.
 *
 * @since 1.3.0
 */
@Provider
public class BatchExceptionMapper implements ExceptionMapper<BatchRuntimeException> {

    @Override
    public Response toResponse(final BatchRuntimeException exception) {
        RestAPILogger.LOGGER.exceptionAccessingRestAPI(exception);
        final Response.Status status;

        if (exception instanceof NoSuchJobExecutionException ||
                exception instanceof NoSuchJobInstanceException ||
                exception instanceof NoSuchJobException ||
                exception instanceof JobExecutionIsRunningException ||
                exception instanceof JobExecutionNotRunningException ||
                exception instanceof JobExecutionNotMostRecentException ||
                exception instanceof JobExecutionAlreadyCompleteException) {
            status = Response.Status.BAD_REQUEST;
        } else if (exception instanceof JobSecurityException) {
            status = Response.Status.FORBIDDEN;
        } else {
            status = Response.Status.INTERNAL_SERVER_ERROR;
        }

        final Response response = Response.status(status).entity(new BatchExceptionEntity(exception)).build();
        return response;
    }
}
