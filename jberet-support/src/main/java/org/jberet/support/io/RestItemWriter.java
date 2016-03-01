/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
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

import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemWriter;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jberet.support._private.SupportMessages;

/**
 * An implementation of {@code ItemWriter} that posts data items to REST resource.
 * <p>
 * Usage example:
 * <pre>
 * &lt;chunk&gt;
 *      ...
 *      &lt;writer ref="restItemWriter"&gt;
 *          &lt;properties&gt;
 *              &lt;property name="restUrl" value="http://localhost:8080/appName/rest-api/movies?param1=value1"/&gt;
 *          &lt;/properties&gt;
 *      &lt;/writer&gt;
 *  &lt;/chunk&gt;
 * </pre>
 *
 * @see RestItemReader
 * @see RestItemReaderWriterBase
 *
 * @since 1.3.0
 */
@Named
@Dependent
public class RestItemWriter extends RestItemReaderWriterBase implements ItemWriter {
    /**
     * Media type to use in the REST call to write data. Its value should be valid
     * for the target REST resource. If not specified, this property defaults to
     * {@value javax.ws.rs.core.MediaType#APPLICATION_JSON}.
     */
    @Inject
    @BatchProperty
    protected String mediaType;

    /**
     * The {@code javax.ws.rs.core.MediaType} value based on {@link #mediaType}
     * batch property. Its value is initialized in {@link #open(Serializable)}.
     */
    protected MediaType mediaTypeInstance;

    /**
     * During the writer opening, the REST client is instantiated, and
     * {@code checkpoint} is ignored.
     *
     * @param checkpoint checkpoint info ignored
     * @throws Exception if error occurs
     */
    @Override
    public void open(final Serializable checkpoint) throws Exception {
        super.open(checkpoint);
        if(httpMethod == null) {
            httpMethod = HttpMethod.POST;
        } else {
            httpMethod = httpMethod.toUpperCase(Locale.ENGLISH);
            if (!HttpMethod.POST.equals(httpMethod) && !HttpMethod.PUT.equals(httpMethod)) {
                throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, httpMethod, "httpMethod");
            }
        }
        if (mediaType != null) {
            mediaTypeInstance = MediaType.valueOf(mediaType);
        } else {
            mediaTypeInstance = MediaType.APPLICATION_JSON_TYPE;
        }
    }

    @Override
    public void writeItems(final List<Object> items) throws Exception {
        final WebTarget target = client.target(restUrl);
        final Entity<List<Object>> entity = Entity.entity(items, mediaTypeInstance);

        final Response response;
        if (HttpMethod.POST.equals(httpMethod)) {
            response = target.request().post(entity);
        } else {
            response = target.request().put(entity);
        }
        final Response.Status.Family statusFamily = response.getStatusInfo().getFamily();
        if (statusFamily == Response.Status.Family.CLIENT_ERROR ||
                statusFamily == Response.Status.Family.SERVER_ERROR) {
            throw SupportMessages.MESSAGES.restApiFailure(response.getStatus(),
                    response.getStatusInfo().getReasonPhrase(), response.getEntity());
        }
    }

    /**
     * Returns writer checkpoint info, always null.
     *
     * @return writer checkpoint info, always null
     */
    @Override
    public Serializable checkpointInfo() {
        return null;
    }
}
