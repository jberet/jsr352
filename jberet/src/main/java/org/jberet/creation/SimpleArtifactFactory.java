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

package org.jberet.creation;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Map;
import javax.batch.api.BatchProperty;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

import org.jberet.job.Properties;
import org.jberet.metadata.ApplicationMetaData;
import org.jberet.util.BatchUtil;

import static org.jberet.util.BatchLogger.LOGGER;

public final class SimpleArtifactFactory implements ArtifactFactory {
    public void initialize() throws Exception {
    }

    @Override
    public Class<?> getArtifactClass(String ref, ClassLoader classLoader, Map<?, ?> data) {
        ApplicationMetaData appData = (ApplicationMetaData) data.get(DataKey.APPLICATION_META_DATA);
        String className = appData.getClassNameForRef(ref);
        Class<?> cls;
        try {
            cls = classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw LOGGER.failToCreateArtifact(e, ref);
        }
        return cls;
    }

    @Override
    public Object create(String ref, Class<?> cls, ClassLoader classLoader, Map<?, ?> data) throws Exception {
        if (cls == null) {
            cls = getArtifactClass(ref, classLoader, data);
        }
        Object obj = cls.newInstance();
        doInjection(obj, cls, data);
        return obj;
    }

    @Override
    public void destroy(Object instance) throws Exception {

    }

    private void doInjection(Object obj, Class<?> cls, Map<?, ?> data) throws Exception {
        Properties batchProps = (Properties) data.get(DataKey.BATCH_PROPERTY);
        boolean hasBatchProps = batchProps != null && batchProps.getProperty().size() > 0;
        while (cls != null && cls != Object.class && !cls.getPackage().getName().startsWith("javax.batch")) {
            for (Field f : cls.getDeclaredFields()) {
                if (!f.isSynthetic()) {
                    Object fieldVal = null;
                    if (f.getAnnotation(Inject.class) != null) {
                        Class<?> fType = f.getType();
                        if (fType == JobContext.class) {
                            fieldVal = data.get(DataKey.JOB_CONTEXT);
                        } else if (fType == StepContext.class) {
                            //fieldVal may be null when StepContext was not stored in data map, as in job listeners
                            fieldVal = data.get(DataKey.STEP_CONTEXT);
                        } else if (hasBatchProps) {
                            BatchProperty batchPropertyAnn = f.getAnnotation(BatchProperty.class);
                            if (batchPropertyAnn != null) {
                                String propName = batchPropertyAnn.name();
                                if (propName.equals("")) {
                                    propName = f.getName();
                                }
                                fieldVal = BatchUtil.getBatchProperty(batchProps, propName);
                                if ("".equals(fieldVal)) {
                                    fieldVal = null;
                                }
                                if (fType != String.class && fieldVal != null) {
                                    fieldVal = convertFieldValue((String) fieldVal, fType);
                                }
                            }
                        }
                        if (fieldVal != null) {
                            doInjection(obj, f, fieldVal);
                        }
                    }
                }
            }
            cls = cls.getSuperclass();
        }
    }

    private Object convertFieldValue(String v, Class<?> t) {
        v = v.trim();
        if (t == int.class || t == Integer.class) {
            return Integer.valueOf(v);
        }
        if (t == long.class || t == Long.class) {
            return Long.valueOf(v);
        }
        if (t == double.class || t == Double.class) {
            return Double.valueOf(v);
        }
        if (t == boolean.class || t == Boolean.class) {
            return Boolean.valueOf(v);
        }
        if (t == float.class || t == Float.class) {
            return Float.valueOf(v);
        }
        if (t == char.class || t == Character.class) {
            return v.charAt(0);
        }
        if (t == byte.class || t == Byte.class) {
            return Byte.valueOf(v);
        }
        if (t == short.class || t == Short.class) {
            return Short.valueOf(v);
        }
        return v;
    }

    private void doInjection(final Object obj, final Field field, final Object val) throws Exception {
        if (System.getSecurityManager() == null) {
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            field.set(obj, val);
        } else {
            AccessController.doPrivileged(new SetFieldPrivilegedExceptionAction(field, obj, val));
        }
    }

    private static class SetFieldPrivilegedExceptionAction implements PrivilegedExceptionAction<Void> {
        private final Field field;
        private final Object obj;
        private final Object val;

        public SetFieldPrivilegedExceptionAction(Field field, Object obj, Object val) {
            this.field = field;
            this.obj = obj;
            this.val = val;
        }

        @Override
        public Void run() throws Exception {
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            field.set(obj, val);
            return null;
        }
    }
}
