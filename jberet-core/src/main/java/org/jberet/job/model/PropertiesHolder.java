/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.job.model;

/**
 * An interface to indicate that the implementing class and its corresponding job element can contain
 * {@link org.jberet.job.model.Properties}.
 */
public interface PropertiesHolder {
    /**
     * Gets {@code org.jberet.job.model.Properties} in the current job model type.
     *
     * @return {@code org.jberet.job.model.Properties}
     */
    Properties getProperties();

    /**
     * Sets {@code org.jberet.job.model.Properties} to the current job model type.
     *
     * @param properties {@code org.jberet.job.model.Properties}
     */
    void setProperties(final Properties properties);
}
