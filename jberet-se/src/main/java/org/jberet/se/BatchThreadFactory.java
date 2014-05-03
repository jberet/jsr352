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

package org.jberet.se;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class BatchThreadFactory implements ThreadFactory {
    final AtomicInteger threadNumber = new AtomicInteger(1);
    final String namePrefix = "jberet-";

    @Override
    public Thread newThread(final Runnable r) {
        final Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
        t.setDaemon(true);
        //some libs rely on TCCL
        //t.setContextClassLoader(null);
        return t;
    }
}