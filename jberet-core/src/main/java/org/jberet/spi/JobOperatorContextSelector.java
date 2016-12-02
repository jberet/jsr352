/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
