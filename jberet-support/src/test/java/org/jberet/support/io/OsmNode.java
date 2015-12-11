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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "node")
@JsonIgnoreProperties(ignoreUnknown = true)
public class OsmNode {

    @JacksonXmlProperty(isAttribute = true)
    private String id;

    @JacksonXmlProperty(isAttribute = true)
    private String version;

    @JacksonXmlProperty(isAttribute = true)
    private Date timestamp;

    @JacksonXmlProperty(isAttribute = true)
    private String uid;

    @JacksonXmlProperty(isAttribute = true)
    private String user;

    @JacksonXmlProperty(isAttribute = true)
    private String changeset;

    @JacksonXmlProperty(isAttribute = true)
    private double lat;

    @JacksonXmlProperty(isAttribute = true)
    private double lon;

    @JacksonXmlProperty(localName = "tag")
    @JacksonXmlElementWrapper(useWrapping = false, localName = "tag")
    private List<Tag> tags = new ArrayList<Tag>();

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(final String uid) {
        this.uid = uid;
    }

    public String getUser() {
        return user;
    }

    public void setUser(final String user) {
        this.user = user;
    }

    public String getChangeset() {
        return changeset;
    }

    public void setChangeset(final String changeset) {
        this.changeset = changeset;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(final double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(final double lon) {
        this.lon = lon;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void addTag(final Tag tag) {
        this.tags.add(tag);
    }

    public void setTags(final List<Tag> tags) {
        this.tags = tags;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof OsmNode)) return false;

        final OsmNode osmNode = (OsmNode) o;

        if (id != null ? !id.equals(osmNode.id) : osmNode.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("OsmNode{");
        sb.append("id='").append(id).append('\'');
        sb.append(", version='").append(version).append('\'');
        sb.append(", timestamp=").append(timestamp);
        sb.append(", uid='").append(uid).append('\'');
        sb.append(", user='").append(user).append('\'');
        sb.append(", changeset='").append(changeset).append('\'');
        sb.append(", lat=").append(lat);
        sb.append(", lon=").append(lon);
        sb.append(", tags=").append(tags);
        sb.append('}');
        return sb.toString();
    }

    @JacksonXmlRootElement(localName = "tag")
    public static class Tag {
        @JacksonXmlProperty(isAttribute = true)
        private String k;

        @JacksonXmlProperty(isAttribute = true)
        private String v;

        public String getK() {
            return k;
        }

        public void setK(final String k) {
            this.k = k;
        }

        public String getV() {
            return v;
        }

        public void setV(final String v) {
            this.v = v;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (!(o instanceof Tag)) return false;

            final Tag tag = (Tag) o;

            if (!k.equals(tag.k)) return false;
            if (!v.equals(tag.v)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = k.hashCode();
            result = 31 * result + v.hashCode();
            return result;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Tag{");
            sb.append("k='").append(k).append('\'');
            sb.append(", v='").append(v).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }
}
