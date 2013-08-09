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

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.batch.operations.JobStartException;

import org.jberet.job.model.Flow;
import org.jberet.job.model.Job;
import org.jberet.job.model.Step;
import org.jboss.marshalling.cloner.ClonerConfiguration;
import org.jboss.marshalling.cloner.ObjectCloner;
import org.jboss.marshalling.cloner.ObjectClonerFactory;
import org.jboss.marshalling.cloner.ObjectCloners;

public class BatchUtil {
    public static final String NL = System.getProperty("line.separator");
    private static ExecutorService executorService = Executors.newCachedThreadPool(new BatchThreadFactory());
    private static final ObjectClonerFactory clonerFactory = ObjectCloners.getSerializingObjectClonerFactory();
    private static final ObjectCloner cloner = clonerFactory.createCloner(new ClonerConfiguration());

    public static ClassLoader getBatchApplicationClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = BatchUtil.class.getClassLoader();
        }
        return cl;
    }

    public static ExecutorService getExecutorService() {
        return executorService;
    }

    public static String propertiesToString(Properties properties) {
        if (properties == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String key : properties.stringPropertyNames()) {
            sb.append(key).append('=').append(properties.getProperty(key)).append(NL);
        }
        return sb.toString();
    }

    /**
     * Produces a StringBuilder containing contactinated id of elements.
     *
     * @param elements step, job, or flow elements, and all elements are of the same type.  Either all elements are job,
     *                 or all elements are step, or all elements are flow
     * @return a StringBuilder whose string value is in the form: a -> b -> c ->
     */
    public static StringBuilder toElementSequence(List<?> elements) {
        StringBuilder sb = new StringBuilder();
        for (Object e : elements) {
            if (e instanceof Step) {
                sb.append(((Step) e).getId());
            } else if (e instanceof Job) {
                sb.append(((Job) e).getId());
            } else if (e instanceof Flow) {
                sb.append(((Flow) e).getId());
            }
            sb.append(" -> ");
        }
        return sb;
    }

    public static <T> T clone(T original) throws JobStartException {
        try {
            cloner.reset();
            return (T) cloner.clone(original);
        } catch (IOException e) {
            throw new JobStartException(e);
        } catch (ClassNotFoundException e) {
            throw new JobStartException(e);
        }
    }

}
