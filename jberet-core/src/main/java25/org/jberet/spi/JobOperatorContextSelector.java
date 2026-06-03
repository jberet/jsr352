/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.spi;

/**
 * A selector for the {@link JobOperatorContext}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public interface JobOperatorContextSelector {

    /**
     * Returns the job operator context for the current environment.
     *
     * @return the job operator context
     */
    JobOperatorContext getJobOperatorContext();
}
