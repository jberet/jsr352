/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.samples.wildfly.schedule.executor;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.jberet.schedule.JobScheduler;

/**
 * {@code ServletContextListener} class that demonstrates how to configure
 * {@code org.jberet.schedule.JobScheduler} to use a custom
 * {@code ScheduledExecutorService} instead of the default resource in
 * the application server.
 * <p>
 * The {@link #contextInitialized(ServletContextEvent)} method retrieves this
 * configuration info from web.xml. Note that in this sample, the custom
 * {@code ScheduledExecutorService} resource refers to the same resource as
 * the default resource.
 * <p>
 *
 */
@WebListener
public class ContextListener1 implements ServletContextListener {
    public static final String SCHEDULED_EXECUTOR_SERVICE_LOOKUP = "org.jberet.schedule.ScheduledExecutorService";

    @Override
    public void contextInitialized(final ServletContextEvent servletContextEvent) {
        final ServletContext servletContext = servletContextEvent.getServletContext();
        final String lookup = servletContext.getInitParameter(SCHEDULED_EXECUTOR_SERVICE_LOOKUP);
        System.out.printf("Got %s = %s%n", SCHEDULED_EXECUTOR_SERVICE_LOOKUP, lookup);

        // to initialize job scheduler with our configuration
        JobScheduler.getJobScheduler(null, null, lookup);
    }

    @Override
    public void contextDestroyed(final ServletContextEvent servletContextEvent) {

    }
}
