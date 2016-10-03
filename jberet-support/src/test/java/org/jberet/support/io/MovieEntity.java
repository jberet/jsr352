/*
 * Copyright (c) 2014-2015 Red Hat, Inc. and/or its affiliates.
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
import java.util.Date;
import javax.persistence.AccessType;
import javax.persistence.Basic;
import javax.persistence.Entity;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@Entity
@javax.persistence.Access(AccessType.FIELD)
public class MovieEntity extends MovieBase implements Serializable {
    private static final long serialVersionUID = -8771060045002998154L;

    @Basic
    @JacksonXmlProperty(isAttribute = true)
    private Date opn;

    public Date getOpn() {
        return opn;
    }

    public void setOpn(final Date opn) {
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
