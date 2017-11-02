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

package org.jberet.vertx.rest.sample;

import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;
import org.jberet.vertx.rest.JBeretRouterConfig;

/**
 *
 * @since 1.3.0.Beta7
 */
public class TestServer extends AbstractVerticle {
    // Convenience method so you can run it in your IDE
    public static void main(String[] args) {
        Runner.runExample(TestServer.class, null);
    }

    @Override
    public void start() throws Exception {
        Router router = Router.router(vertx);
        JBeretRouterConfig.config(router);

        vertx.createHttpServer().requestHandler(router::accept).listen(8080);
    }
}
