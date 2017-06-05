/*
 * Copyright (c) 2017 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.vertx.cluster;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import org.jberet.runtime.context.StepContextImpl;
import org.jberet.runtime.runner.StepExecutionRunner;
import org.jberet.spi.PartitionHandler;
import org.jberet.spi.PartitionHandlerFactory;
import org.jberet.vertx.cluster._private.VertxClusterLogger;
import org.jberet.vertx.cluster._private.VertxClusterMessages;

public class VertxPartitionHandlerFactory implements PartitionHandlerFactory {
    private Vertx vertx;
    private static final int clusterInitTimeout = 5;

    public VertxPartitionHandlerFactory() {
        final Context context = Vertx.currentContext();
        if (context != null) {
            vertx = context.owner();
            if (!vertx.isClustered()) {
                throw VertxClusterMessages.MESSAGES.vertxNotClustered(vertx);
            }
        } else {
            final CountDownLatch countDownLatch = new CountDownLatch(1);
            Vertx.clusteredVertx(new VertxOptions(), res -> {
                if (res.succeeded()) {
                    vertx = res.result();
                    countDownLatch.countDown();
                } else {
                    throw VertxClusterMessages.MESSAGES.failToInitVertxCluster(res.cause());
                }
            });
            try {
                if (!countDownLatch.await(clusterInitTimeout, TimeUnit.MINUTES)) {
                    throw VertxClusterMessages.MESSAGES.failToInitVertxClusterTimeout(clusterInitTimeout, TimeUnit.MINUTES);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            VertxClusterLogger.LOGGER.createdClusteredVertx(vertx);
        }
    }

    @Override
    public PartitionHandler createPartitionHandler(final StepContextImpl stepContext,
                                                   final StepExecutionRunner stepExecutionRunner) {
        return new VertxPartitionHandler(stepContext, vertx);
    }
}
