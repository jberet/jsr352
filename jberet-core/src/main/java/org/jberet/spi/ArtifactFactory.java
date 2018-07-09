/*
 * Copyright (c) 2012-2013 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.spi;

public interface ArtifactFactory {
    /**
     * The create method creates an instance
     * corresponding to a ref value from a Job XML.
     *
     * @param ref value from Job XML
     * @param cls the class type of the target artifact.  Either ref or cls may be specified.
     * @param classLoader the class loader for loading the artifact class
     * @return instance corresponding to ref value
     * @throws Exception if instance cannot be created.
     */
    public Object create(String ref, Class<?> cls, ClassLoader classLoader) throws Exception;

    /**
     * The destroy method destroys an instance created
     * by this factory.
     *
     * @param instance to destroy
     */
    public void destroy(Object instance);

    /**
     * Gets the class type of the artifact represented by ref.
     * @param ref the ref name of the artifact
     * @param classLoader the class loader for loading the artifact class
     * @return the Class type of the artifact represented by ref
     */
    public Class<?> getArtifactClass(String ref, ClassLoader classLoader);
}