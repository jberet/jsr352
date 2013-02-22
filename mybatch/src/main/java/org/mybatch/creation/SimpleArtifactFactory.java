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

package org.mybatch.creation;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Map;
import javax.batch.annotation.BatchProperty;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

import org.mybatch.job.Properties;
import org.mybatch.metadata.ApplicationMetaData;
import org.mybatch.util.BatchUtil;

import static org.mybatch.util.BatchLogger.LOGGER;

public final class SimpleArtifactFactory implements ArtifactFactory {
    public void initialize() throws Exception {

    }

    public Object create(String ref, ClassLoader classLoader, Map<?, ?> data) throws Exception {
        ApplicationMetaData appData = (ApplicationMetaData) data.get(DataKey.APPLICATION_META_DATA);
        String className = appData.getClassNameForRef(ref);
        if (className == null) {
            throw LOGGER.failToCreateArtifact(null, ref);
        }
        Class<?> cls = classLoader.loadClass(className);
        Object obj = cls.newInstance();
        doInjection(obj, cls, data);
        return obj;
    }

    public void destroy(Object instance) throws Exception {

    }

    private void doInjection(Object obj, Class<?> cls, Map<?, ?> data) throws Exception {
        Properties batchProps = (Properties) data.get(DataKey.BATCH_PROPERTY);
        boolean hasBatchProps = batchProps != null && batchProps.getProperty().size() > 0;
        while (cls != null && cls != Object.class && !cls.getPackage().getName().startsWith("javax.batch")) {
            for (Field f : cls.getDeclaredFields()) {
                if (!f.isSynthetic()) {
                    Object fieldVal = null;
                    if (hasBatchProps) {
                        BatchProperty batchPropertyAnn = f.getAnnotation(BatchProperty.class);
                        if (batchPropertyAnn != null) {
                            String propName = batchPropertyAnn.name();
                            if (propName.equals("")) {
                                propName = f.getName();
                            }
                            fieldVal = BatchUtil.getBatchProperty(batchProps, propName);
                            doInjection(obj, f, fieldVal);
                            continue;
                        }
                    }

                    Inject injectAnn = f.getAnnotation(Inject.class);
                    if (injectAnn != null) {
                        if (f.getType() == JobContext.class) {
                            fieldVal = data.get(DataKey.JOB_CONTEXT);
                        } else if (f.getType() == StepContext.class) {
                            //fieldVal may be null when StepContext was not stored in data map, as in job listeners
                            fieldVal = data.get(DataKey.STEP_CONTEXT);
                        }
                        doInjection(obj, f, fieldVal);
                    }
                }
            }
            cls = cls.getSuperclass();
        }
    }

    private void doInjection(final Object obj, final Field field, final Object val) throws Exception {
        if (val == null) {
            return;
        }
        if (System.getSecurityManager() == null) {
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            field.set(obj, val);
        } else {
            AccessController.doPrivileged(
                    new PrivilegedExceptionAction<Void>() {
                        public Void run() throws Exception {
                            if (!field.isAccessible()) {
                                field.setAccessible(true);
                            }
                            field.set(obj, val);
                            return null;
                        }
                    });
        }
    }
}
