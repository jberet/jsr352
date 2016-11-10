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

package org.jberet.support.io;

import javax.persistence.MappedSuperclass;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@MappedSuperclass
public abstract class MovieBase {
    public enum Rating {G, PG, PG13, R}

    @JacksonXmlProperty(isAttribute = true)
    int rank;

    @JacksonXmlProperty(isAttribute = true)
    String tit;

    @JacksonXmlProperty(isAttribute = true)
    double grs;

    @JacksonXmlProperty(isAttribute = true)
    Rating rating;

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

    public Rating getRating() {
        return rating;
    }

    public void setRating(final Rating rating) {
        this.rating = rating;
    }
}
