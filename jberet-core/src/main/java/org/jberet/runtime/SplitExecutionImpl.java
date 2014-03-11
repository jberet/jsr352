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
