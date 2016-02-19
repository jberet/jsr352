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

package org.jberet.samples.wildfly.restwriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.jberet.samples.wildfly.common.Movie;

/**
 * REST resource class for {@link Movie}.
 */
@Path("/movies")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class MoviesResource {
    private static final String moviesKey = "movies-";

    /**
     * {@code javax.servlet.ServletContext} from which to retrieve all movie data.
     */
    @Context
    private ServletContext servletContext;

    @SuppressWarnings("unchecked")
    @GET
    public List<Movie> getMovies(final @QueryParam("testName") String testName) {
        return (List<Movie>) servletContext.getAttribute(getMoviesKey(testName));
    }

    @DELETE
    public void removeMovies(final @QueryParam("testName") String testName) {
        servletContext.removeAttribute(getMoviesKey(testName));
    }

    @POST
    public void addMovies(final @QueryParam("testName") String testName,
                          final Movie[] movies) {
        addMovie0(testName, Arrays.asList(movies));
        System.out.printf("Adding Movie[] for testName %s: %s elements%n%s%n",
                testName, movies.length, Arrays.toString(movies));
    }

    @Path("list")
    @POST
    public void addMoviesList(final @QueryParam("testName") String testName,
                              final List<Movie> movies) {
        addMovie0(testName, movies);
        System.out.printf("Adding List<Movie> for testName %s: %s elements%n%s%n",
                testName, movies.size(), movies);
    }

    @Path("collection")
    @POST
    public void addMoviesCollection(final @QueryParam("testName") String testName,
                                    final Collection<Movie> movies) {
        addMovie0(testName, movies);
        System.out.printf("Adding Collection<Movie> for testName %s: %s elements%n%s%n",
                testName, movies.size(), movies);
    }

    /**
     * Adds movies, using {@code ServletContext} attribute as a storage for testing purpose.
     *
     * @param testName the current test name, used as part of the key for {@code ServletContext} attribute
     * @param movies a collection of {@code Movie}
     */
    @SuppressWarnings("unchecked")
    private void addMovie0(final String testName, final Collection<Movie> movies) {
        final String key = getMoviesKey(testName);
        List<Movie> existingMovies = (List<Movie>) servletContext.getAttribute(key);
        if (existingMovies == null) {
            existingMovies = new ArrayList<Movie>();
            servletContext.setAttribute(key, existingMovies);
        }
        existingMovies.addAll(movies);
    }

    private String getMoviesKey(final String testName) {
        return moviesKey + testName;
    }
}
