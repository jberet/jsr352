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

package org.jberet.creation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
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
        doInjection(obj, cls, classLoader, data);
        invokeAnnotatedLifecycleMethod(obj, cls, PostConstruct.class);
        return obj;
    }

    @Override
    public void destroy(Object instance) {
        if (instance != null) {
            try {
                invokeAnnotatedLifecycleMethod(instance, instance.getClass(), PreDestroy.class);
            } catch (Exception e) {
                LOGGER.failToDestroyArtifact(e, instance);
            }
        }
    }

    private void doInjection(final Object obj, Class<?> cls, final ClassLoader classLoader, final Map<?, ?> data) throws Exception {
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
                                String sVal = BatchUtil.getBatchProperty(batchProps, propName);
                                if (sVal != null) {
                                    if (sVal.length() == 0) {
                                        fieldVal = null;
                                    } else if (!fType.isAssignableFrom(String.class)) {
                                        fieldVal = ValueConverter.convertFieldValue(sVal, fType, f, classLoader);
                                    } else {
                                        fieldVal = sVal;
                                    }
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

    private void invokeAnnotatedLifecycleMethod(final Object obj, Class<?> cls, final Class<? extends Annotation> annCls) throws Exception{
        List<Method> lifecycleMethods = new ArrayList<Method>();
        while (cls != null && cls != Object.class && !cls.getPackage().getName().startsWith("javax.batch")) {
            Method[] methods = cls.getDeclaredMethods();
            for (Method m : methods) {
                if (m.getAnnotation(annCls) != null) {  //the lifecyle annotation is present
                    int modifiers = m.getModifiers();
                    String mName = m.getName();
                    if (Modifier.isPrivate(modifiers)) {
                        lifecycleMethods.add(m);
                    } else {
                        boolean alreadyAdded = false;
                        for (Method lm : lifecycleMethods) {
                            if (lm.getName().equals(mName)) {
                                if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) {
                                    alreadyAdded = true;
                                    break;
                                } else { // package default access
                                    if (m.getDeclaringClass().getPackage().getName().equals(lm.getDeclaringClass().getPackage().getName())) {
                                        alreadyAdded = true;
                                        break;
                                    }
                                    //there can be multiple methods of the same name in lifecycleMethods, some are its
                                    //super method and some are not.  So need to continue.
                                }
                            }
                        }
                        if (!alreadyAdded) {
                            lifecycleMethods.add(m);
                        }
                    }
                }
            }
            cls = cls.getSuperclass();
        }
        if (annCls == PostConstruct.class) {
            Collections.reverse(lifecycleMethods);
        }
        for(Method m : lifecycleMethods) {
            if (System.getSecurityManager() == null) {
                if (!m.isAccessible()) {
                    m.setAccessible(true);
                }
                m.invoke(obj);
            } else {
                AccessController.doPrivileged(new InvokeMethodPrivilegedExceptionAction(m, obj));
            }
        }
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

    private static class SetFieldPrivilegedExceptionAction implements java.security.PrivilegedExceptionAction<Void> {
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

    private static class InvokeMethodPrivilegedExceptionAction implements PrivilegedExceptionAction<Object> {
        private final Method method;
        private final Object obj;

        public InvokeMethodPrivilegedExceptionAction(Method method, Object obj) {
            this.method = method;
            this.obj = obj;
        }

        @Override
        public Object run() throws Exception {
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
            return method.invoke(obj);
        }
    }
}
