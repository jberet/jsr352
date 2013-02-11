/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
 
package org.mybatch.util;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.mybatch.job.Job;
import org.mybatch.job.Listener;
import org.mybatch.job.Listeners;
import org.mybatch.job.Property;
import org.mybatch.job.Step;

public class BatchUtil {

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

    public static Properties toJavaUtilProperties(org.mybatch.job.Properties props) {
        Properties result = new Properties();
        if (props != null) {
            for (org.mybatch.job.Property p : props.getProperty()) {
                result.setProperty(p.getName(), p.getValue());
            }
        }
        return result;
    }

    public static String getBatchProperty(org.mybatch.job.Properties batchProps, String key) {
        if (batchProps != null) {
            for (Property p : batchProps.getProperty()) {
                if (p.getName().equals(key)) {
                    return p.getValue();
                }
            }
        }
        return null;
    }

    public static boolean propertiesContains(org.mybatch.job.Properties props, String key) {
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
}
