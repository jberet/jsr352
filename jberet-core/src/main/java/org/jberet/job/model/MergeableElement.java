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

package org.jberet.job.model;

import java.io.Serializable;

/**
 * Base class for job model types that may be merged with parent element when resolving its inheritance, for instance,
 * <ul>
 *     <li>{@link Properties}
 *     <li>{@link ExceptionClassFilter}
 *     <li>{@link Listeners}
 * </ul>
 */
abstract class MergeableElement implements Serializable {
    private static final long serialVersionUID = 8615949849257798431L;

    /**
     * Indicates whether to merge with the counterpart element from the parent job element. Only applicable when the
     * current job element (e.g., a step) has a parent. The default merge value is true.
     */
    private boolean merge = true;

    /**
     * Checks whether to merge with the counterpart element from the parent job element.
     *
     * @return true (default) or false
     */
    boolean isMerge() {
        return merge;
    }

    /**
     * Sets the {@code merge} attribute when the current job element inherits from a parent.
     *
     * @param mergeVal {@code merge} attribute value ("true" or "false") as string
     */
    void setMerge(final String mergeVal) {
        if (mergeVal != null && !mergeVal.toLowerCase().equals("true")) {
            this.merge = false;
        }
    }
}
