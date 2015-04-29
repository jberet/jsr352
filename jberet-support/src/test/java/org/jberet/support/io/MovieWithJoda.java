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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.joda.time.DateTime;

/**
 * A bean that represents the movie data, using joda date, from http://mysafeinfo.com/api/data?list=topmoviesboxoffice2012&format=csv,
 * or http://mysafeinfo.com/api/data?list=topmoviesboxoffice2012&format=xml
 *
 * @see Movie
 */

@JacksonXmlRootElement(localName = "t")
public final class MovieWithJoda extends MovieBase {

    @JacksonXmlProperty(isAttribute = true)
    private DateTime opn;

    public DateTime getOpn() {
        return opn;
    }

    public void setOpn(final DateTime opn) {
        this.opn = opn;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Movie{");
        sb.append("id='").append(id).append('\'');
        sb.append(", rank=").append(rank);
        sb.append(", tit='").append(tit).append('\'');
        sb.append(", grs=").append(grs);
        sb.append(", opn=").append(opn);
        sb.append(", rating=").append(rating);
        sb.append('}');
        return sb.toString();
    }
}
