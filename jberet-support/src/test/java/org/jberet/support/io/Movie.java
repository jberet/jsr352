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

/**
 * A bean that represents the movie data from http://mysafeinfo.com/api/data?list=topmoviesboxoffice2012&format=csv
 */
public final class Movie {
    private int rank;
    private String tit;
    private double grs;
    private Date opn;

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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Movie{");
        sb.append("rank=").append(rank);
        sb.append(", tit='").append(tit).append('\'');
        sb.append(", grs=").append(grs);
        sb.append(", opn=").append(opn);
        sb.append('}');
        return sb.toString();
    }
}
