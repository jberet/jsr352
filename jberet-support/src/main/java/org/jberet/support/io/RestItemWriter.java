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
import java.net.URI;
import java.util.List;
import java.util.Locale;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemWriter;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jberet.support._private.SupportMessages;

/**
 * An implementation of {@code ItemWriter} that posts data items to REST resource.
 * <p/>
 * Usage example:
 * <pre>
 * &lt;chunk&gt;
 *   ...
 * &lt;chunk&gt;
 * </pre>
 *
 * @since 1.3.0
 */
@Named
@Dependent
public class RestItemWriter implements ItemWriter {
    /**
     * The base URI for the REST call. It usually points to a collection resource URI,
     * from which resources may be retrieved via HTTP GET method. The URI may include
     * additional query parameters other than offset (starting position to read) and
     * limit (maximum number of items to return in each response). Query parameter
     * offset and limit are specified by their own batch properties.
     * <p/>
     * For example, {@code http://localhost:8080/restReader/api/movies}
     * <p/>
     * This is a required batch property.
     */
    @Inject
    @BatchProperty
    protected URI restUrl;

    /**
     * HTTP method to use in the REST call to write data. Valid values are
     * {@code POST} and {@code PUT}. If not specified, this property defaults to
     * {@value HttpMethod#POST}.
     */
    @Inject
    @BatchProperty
    protected String httpMethod;

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
     * The class of individual element of the response message entity. For example,
     * <ul>
     * <li>{@code java.lang.String}
     * <li>{@code org.jberet.samples.wildfly.common.Movie}
     * </ul>
     */
    @Inject
    @BatchProperty
    protected Class beanType;

    /**
     * REST client {@code javax.ws.rs.client.Client}, which is instantiated
     * in {@link #open(Serializable)} and closed in {@link #close()}.
     */
    protected Client client;

    /**
     * During the reader opening, the REST client is instantiated, and
     * {@code checkpoint}, if any, is used to position the reader properly.
     *
     * @param checkpoint checkpoint info, null for the first invocation in a new job execution
     * @throws Exception if error occurs
     */
    @Override
    public void open(final Serializable checkpoint) throws Exception {
        client = ClientBuilder.newClient();

        if (restUrl == null) {
            throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, null, "restUrl");
        }
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
     * Returns reader checkpoint info (int number), which is the last successfully read position.
     *
     * @return reader checkpoint info as int
     */
    @Override
    public Serializable checkpointInfo() {
        return null;
    }

    /**
     * closes the REST client and sets it to null.
     */
    @Override
    public void close() {
        if (client != null) {
            client.close();
            client = null;
        }
    }
}
