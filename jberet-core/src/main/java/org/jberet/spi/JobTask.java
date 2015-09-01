/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jberet.spi;

/**
 * A task to be used with the {@link JobExecutor} for leaving permits in a thread-pool open if required by the task
 * being executed.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public interface JobTask extends Runnable {

    /**
     * The number of threads in the pool required to be remaining before this task should execute.
     *
     * @return the number of threads that should be available
     */
    int getRequiredRemainingPermits();
}
