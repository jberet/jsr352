/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mybatch.creation;

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
     * @return instance corresponding to ref value
     * @throws Exception if instance cannot be created.
     */
    public Object create(String ref, Map<?, ?> data) throws Exception;

    /**
     * The destroy method destroys an instance created
     * by this factory.
     *
     * @param instance to destroy
     * @throws Exception if instance cannot be destroyed.
     */
    public void destroy(Object instance) throws Exception;

}