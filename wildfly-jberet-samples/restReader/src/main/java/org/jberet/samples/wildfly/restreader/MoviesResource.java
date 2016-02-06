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

package org.jberet.samples.wildfly.restreader;

import java.util.List;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.jberet.samples.wildfly.common.Movie;

@Path("/movies")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class MoviesResource {
    @Context
    private ServletContext servletContext;

    @SuppressWarnings("unchecked")
    @GET
    public Movie[] getMovies(final @QueryParam("offset") int offset,
                            final @QueryParam("limit") int limit) {
        final List<Movie> allMovies = (List<Movie>) servletContext.getAttribute(ServletContextListener1.moviesKey);
        final List<Movie> resultList = allMovies.subList(offset, Math.min(offset + limit, allMovies.size()));
        System.out.printf("## resultList: %s  %s%n", resultList.size(), resultList);
        return resultList.toArray(new Movie[resultList.size()]);
    }
}
