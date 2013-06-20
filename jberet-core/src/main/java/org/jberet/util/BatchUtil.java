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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.batch.operations.JobStartException;

import org.jberet.job.Flow;
import org.jberet.job.Job;
import org.jberet.job.Listener;
import org.jberet.job.Listeners;
import org.jberet.job.Property;
import org.jberet.job.Step;

public class BatchUtil {
    public static final String NL = System.getProperty("line.separator");
    private static ExecutorService executorService = Executors.newCachedThreadPool(new BatchThreadFactory());

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

    public static Properties getPropertiesFromJobDefinition(Job job) {
        return toJavaUtilProperties(job.getProperties());
    }

    public static Properties getPropertiesFromStepDefinition(Step step) {
        return toJavaUtilProperties(step.getProperties());
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

    public static Properties toJavaUtilProperties(org.jberet.job.Properties props) {
        Properties result = new Properties();
        if (props != null) {
            for (org.jberet.job.Property p : props.getProperty()) {
                String v = p.getValue();
                if (v != null) {  //unresolvable properties have value null
                    result.setProperty(p.getName(), v);
                }
            }
        }
        return result;
    }

    public static String getBatchProperty(org.jberet.job.Properties batchProps, String key) {
        if (batchProps != null) {
            for (Property p : batchProps.getProperty()) {
                if (p.getName().equals(key)) {
                    return p.getValue();
                }
            }
        }
        return null;
    }

    public static boolean propertiesContains(org.jberet.job.Properties props, String key) {
        if (props == null) {
            return false;
        }
        for (Property p : props.getProperty()) {
            if (p.getName().equals(key)) {
                return true;
            }
        }
        return false;
    }

    public static boolean listenersContains(Listeners listeners, Listener listener) {
        if (listeners == null) {
            return false;
        }
        for (Listener l : listeners.getListener()) {
            if (l == listener) {
                return true;
            }
        }
        return false;
    }

    /**
     * Produces a StringBuilder containing contactinated id of elements.
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
        T clone = null;
        ObjectOutputStream oos = null;
        ObjectInputStream ois = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            oos.writeObject(original);
            oos.flush();
            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            ois = new ObjectInputStream(bis);
            clone = (T) ois.readObject();
        } catch (IOException e) {
            throw new JobStartException(e);
        } catch (ClassNotFoundException e) {
            throw new JobStartException(e);
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                    //ignore
                }
            }
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
        return clone;
    }

}
