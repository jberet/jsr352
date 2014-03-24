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

package org.jberet.job.model;

/**
 * An interface to indicate that the implementing class and its corresponding job element can contain
 * {@link org.jberet.job.model.Properties}.
 */
public interface PropertiesHolder {
    public Properties getProperties();

    void setProperties(final Properties properties);
}
