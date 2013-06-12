/*
 * Copyright (c) 2012-2013 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.creation;

import java.util.Map;

public interface ArtifactFactory {
    public static enum DataKey {
        APPLICATION_META_DATA,
        JOB_CONTEXT,
        STEP_CONTEXT,
        BATCH_PROPERTY;
    }

    /**
     * The initialize method is invoked once during the
     * initialization of the batch runtime.
     *
     * @throws Exception if artifact factory cannot be loaded. * The batch runtime responds by issuing an error message * and disabling itself.
     */
    public void initialize() throws Exception;

    /**
     * The create method creates an instance
     * corresponding to a ref value from a Job XML.
     *
     * @param ref value from Job XML
     * @param cls the class type of the target artifact.  Either ref or cls may be specified.
     * @param classLoader the class loader for loading the artifact class
     * @param data a map of key-value pair for creating the artifact
     * @return instance corresponding to ref value
     * @throws Exception if instance cannot be created.
     */
    public Object create(String ref, Class<?> cls, ClassLoader classLoader, Map<?, ?> data) throws Exception;

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
     * @param data a map of key-value pair for creating the artifact
     * @return the Class type of the artifact represented by ref
     */
    public Class<?> getArtifactClass(String ref, ClassLoader classLoader, Map<?, ?> data);
}