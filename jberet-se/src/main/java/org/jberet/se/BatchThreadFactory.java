/*
 * Copyright (c) 2012-2013 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.se;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class BatchThreadFactory implements ThreadFactory {
    final AtomicInteger threadNumber = new AtomicInteger(1);
    final static String namePrefix = "jberet-";

    @Override
    public Thread newThread(final Runnable r) {
        final Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
        t.setDaemon(true);
        //some libs rely on TCCL
        //t.setContextClassLoader(null);
        return t;
    }
}