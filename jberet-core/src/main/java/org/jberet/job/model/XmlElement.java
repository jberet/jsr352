/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.job.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum type to hold all XML element names for job xml and batch.xml. It supports getting an enum instance by its
 * corresponding local XML element string value, and getting the string name of an enum instance.
 *
 * @see XmlAttribute
 */
public enum XmlElement {
    UNKNOWN(null),

    //all elements from job xml, in alphabetical order
    ANALYZER("analyzer"),
    BATCHLET("batchlet"),
    CHECKPOINT_ALGORITHM("checkpoint-algorithm"),
    CHUNK("chunk"),
    COLLECTOR("collector"),
    DECISION("decision"),
    END("end"),
    EXCLUDE("exclude"),
    FAIL("fail"),
    FLOW("flow"),
    INCLUDE("include"),
    JOB("job"),
    LISTENER("listener"),
    LISTENERS("listeners"),
    MAPPER("mapper"),
    NEXT("next"),
    NO_ROLLBACK_EXCEPTION_CLASSES("no-rollback-exception-classes"),
    PARTITION("partition"),
    PLAN("plan"),
    PROCESSOR("processor"),
    PROPERTIES("properties"),
    PROPERTY("property"),
    READER("reader"),
    REDUCER("reducer"),
    RETRYABLE_EXCEPTION_CLASSES("retryable-exception-classes"),
    SCRIPT("script"),
    SKIPPABLE_EXCEPTION_CLASSES("skippable-exception-classes"),
    SPLIT("split"),
    STEP("step"),
    STOP("stop"),
    WRITER("writer"),

    //all elements from batch.xml
    BATCH_ARTIFACTS("batch-artifacts"),
    REF("ref");

    private final String name;

    XmlElement(final String name) {
        this.name = name;
    }

    /**
     * Get the local name of the corresponding XML element.
     *
     * @return the local name
     */
    public String getLocalName() {
        return name;
    }

    private static final Map<String, XmlElement> MAP;

    static {
        final Map<String, XmlElement> map = new HashMap<String, XmlElement>();
        for (final XmlElement element : values()) {
            final String name = element.getLocalName();
            if (name != null) {
                map.put(name, element);
            }
        }
        MAP = map;
    }

    /**
     * Looks up {@code XmlElement} enum instance by the local XML element name.
     *
     * @param localName local name of an XML element
     * @return an instance of {@code XmlElement} enum type for {@code localName}, and {@link #UNKNOWN} if no match
     */
    public static XmlElement forName(final String localName) {
        final XmlElement element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }
}
