/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.runtime;

public final class SplitExecutionImpl extends AbstractExecution {
    private static final long serialVersionUID = 4620474075765349318L;
    private final String splitId;

    public SplitExecutionImpl(final String splitId) {
        this.splitId = splitId;
    }

    public String getSplitId() {
        return splitId;
    }

}
