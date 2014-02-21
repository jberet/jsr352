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
 
package org.samples.wildfly.jberet.common;

import java.util.Date;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A bean that represents the movie data from http://mysafeinfo.com/api/data?list=topmoviesboxoffice2012&format=csv,
 * or http://mysafeinfo.com/api/data?list=topmoviesboxoffice2012&format=xml
 *
 * Copied from https://github.com/jberet/jsr352/tree/master/jberet-support/src/test/java/org/jberet/support/io, except
 * that this class uses jaxb annotations instead of jackson annotations.
 */

@XmlRootElement(name = "t")
public final class Movie {
    public enum Rating {G, PG, PG13, R}

    @XmlAttribute
    private int rank;

    @XmlAttribute
    private String tit;

    @XmlAttribute
    private double grs;

    @XmlAttribute
    private Date opn;

    @XmlAttribute
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
