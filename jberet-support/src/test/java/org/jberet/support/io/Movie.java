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

import java.util.Date;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * A bean that represents the movie data from http://mysafeinfo.com/api/data?list=topmoviesboxoffice2012&format=csv,
 * or http://mysafeinfo.com/api/data?list=topmoviesboxoffice2012&format=xml
 */

@JacksonXmlRootElement(localName = "t")
public final class Movie {
    public enum Rating {G, PG, PG13, R}

    @JacksonXmlProperty(isAttribute = true)
    private int rank;

    @JacksonXmlProperty(isAttribute = true)
    private String tit;

    @JacksonXmlProperty(isAttribute = true)
    private double grs;

    @JacksonXmlProperty(isAttribute = true)
    private Date opn;

    @JacksonXmlProperty(isAttribute = true)
    private Rating rating;

    public int getRank() {
        return rank;
    }

    public void setRank(final int rank) {
        this.rank = rank;
    }

    public String getTit() {
        return tit;
    }

    public void setTit(final String tit) {
        this.tit = tit;
    }

    public double getGrs() {
        return grs;
    }

    public void setGrs(final double grs) {
        this.grs = grs;
    }

    public Date getOpn() {
        return opn;
    }

    public void setOpn(final Date opn) {
        this.opn = opn;
    }

    public Rating getRating() {
        return rating;
    }

    public void setRating(final Rating rating) {
        this.rating = rating;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Movie{");
        sb.append("rank=").append(rank);
        sb.append(", tit='").append(tit).append('\'');
        sb.append(", grs=").append(grs);
        sb.append(", opn=").append(opn);
        sb.append(", rating=").append(rating);
        sb.append('}');
        return sb.toString();
    }
}
