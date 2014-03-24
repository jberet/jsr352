/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.job.model;

import java.util.HashMap;
import java.util.Map;

public enum XmlAttribute {
    UNKNOWN(null),

    //attributes from job xml & batch.xml, in alphabetical order
    ABSTRACT("abstract"),
    ALLOW_START_IF_COMPLETE("allow-start-if-complete"),
    CHECKPOINT_POLICY("checkpoint-policy"),
    CLASS("class"),
    EXIT_STATUS("exit-status"),
    ID("id"),
    ITEM_COUNT("item-count"),
    JSL_NAME("jsl-name"),
    MERGE("merge"),
    NAME("name"),
    NEXT("next"),
    ON("on"),
    PARENT("parent"),
    PARTITION("partition"),
    PARTITIONS("partitions"),
    REF("ref"),
    RESTART("restart"),
    RESTARTABLE("restartable"),
    RETRY_LIMIT("retry-limit"),
    SKIP_LIMIT("skip-limit"),
    START_LIMIT("start-limit"),
    THREADS("threads"),
    TIME_LIMIT("time-limit"),
    TO("to"),
    VALUE("value");

    private final String name;

    XmlAttribute(final String name) {
        this.name = name;
    }

    /**
     * Get the local name of this attribute.
     *
     * @return the local name
     */
    public String getLocalName() {
        return name;
    }

    private static final Map<String, XmlAttribute> MAP;

    static {
        final Map<String, XmlAttribute> map = new HashMap<String, XmlAttribute>();
        for (final XmlAttribute attribute : values()) {
            final String name = attribute.getLocalName();
            if (name != null) {
                map.put(name, attribute);
            }
        }
        MAP = map;
    }

    public static XmlAttribute forName(final String localName) {
        final XmlAttribute attribute = MAP.get(localName);
        return attribute == null ? UNKNOWN : attribute;
    }
}
