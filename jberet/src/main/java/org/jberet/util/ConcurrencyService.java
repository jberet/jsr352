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

package org.jberet.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ConcurrencyService {

    private static ExecutorService executorService = Executors.newCachedThreadPool(new BatchThreadFactory());

    public static ExecutorService getExecutorService() {
        return executorService;
    }

    public static Future<?> submit(Runnable r) {
        return executorService.submit(r);
    }

    public static <V> Future<V> submit(Callable<V> c) {
        return executorService.submit(c);
    }

    public static void shutdown() {
        executorService.shutdown();
    }
}
