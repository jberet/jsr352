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
import javax.batch.api.BatchProperty;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.jberet.support._private.SupportMessages;

/**
 * The base class for {@link RestItemReader} and {@link RestItemWriter}.
 *
 * @since 1.3.0
 */
public abstract class RestItemReaderWriterBase {
    /**
     * The base URI for the REST call. It usually points to a collection resource URI.
     * For {@link RestItemReader}, data may be retrieved via HTTP GET or less commonly
     * DELETE method. The URI may include additional query parameters other than
     * offset (starting position to read) and limit (maximum number of items to return
     * in each response). Query parameter offset and limit are specified by their own
     * batch properties.
     * <p>
     * For example, {@code http://localhost:8080/restReader/api/movies}
     * <p>
     * For {@link RestItemWriter}, data may be submitted via HTTP POST or PUT method.
     * The URI may include additional query parameters.
     * <p>
     * For example, {@code http://localhost:8080/restReader/api/movies?param1=value1}
     * <p>
     * This is a required batch property.
     */
    @Inject
    @BatchProperty
    protected URI restUrl;

    /**
     * HTTP method to use in the REST call to read or write data. Its value should
     * corresponds to the media types accepted by the target REST resource.
     * <p>
     * For {@link RestItemReader}, valid values are {@value javax.ws.rs.HttpMethod#GET} and
     * less commonly {@value javax.ws.rs.HttpMethod#DELETE}. If not specified, this property
     * defaults to {@value javax.ws.rs.HttpMethod#GET}.
     * <p>
     * For {@link RestItemWriter}, valid values are {@value javax.ws.rs.HttpMethod#POST} and
     * {@value javax.ws.rs.HttpMethod#PUT}.
     * If not specified, this property defaults to {@value javax.ws.rs.HttpMethod#POST}.
     */
    @Inject
    @BatchProperty
    protected String httpMethod;

    /**
     * REST client {@code javax.ws.rs.client.Client}, which is instantiated
     * in {@link #open(Serializable)} and closed in {@link #close()}.
     */
    protected Client client;

    /**
     * During the writer opening, the REST client is instantiated.
     *
     * @param checkpoint checkpoint info
     * @throws Exception if error occurs
     */
    public void open(final Serializable checkpoint) throws Exception {
        client = ClientBuilder.newClient();

        if (restUrl == null) {
            throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, null, "restUrl");
        }
    }

    /**
     * closes the REST client and sets it to null.
     */
    public void close() {
        if (client != null) {
            client.close();
            client = null;
        }
    }
}
