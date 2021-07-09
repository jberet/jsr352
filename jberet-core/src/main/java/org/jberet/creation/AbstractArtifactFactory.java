/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.creation;

import static org.jberet._private.BatchLogger.LOGGER;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.runtime.context.JobContext;
import jakarta.batch.runtime.context.StepContext;
import org.jberet.job.model.Properties;
import org.jberet.runtime.context.JobContextImpl;
import org.jberet.runtime.context.StepContextImpl;
import org.jberet.spi.ArtifactFactory;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * An abstract implementation of an {@link ArtifactFactory} which contains some helper methods for dealing injecting
 * data if CDI is not available or the class was created from the job XML using the fully qualified class name.
 * <p>
 * Note that if subclasses over the {@link #destroy(Object)} method, they should invoke {@code super.destroy(instance)}
 * if the instance was not destroyed by another means, e.g. releasing the CDI context.
 * </p>
 */
public abstract class AbstractArtifactFactory implements ArtifactFactory {
    @Override
    public void destroy(final Object instance) {
        if (instance != null) {
            try {
                invokeAnnotatedLifecycleMethod(instance, instance.getClass(), PreDestroy.class);
            } catch (Exception e) {
                LOGGER.failToDestroyArtifact(e, instance);
            }
        }
    }

    protected void doInjection(final Object obj, Class<?> cls,
                             final ClassLoader classLoader,
                             final JobContextImpl jobContext,
                             final StepContextImpl stepContext,
                             final Properties batchProps) throws Exception {
        final boolean hasBatchProps = batchProps != null && batchProps.size() > 0;
        while (cls != null && cls != Object.class && cls.getPackage() != null && !cls.getPackage().getName().startsWith("jakarta.batch")) {
            for (final Field f : cls.getDeclaredFields()) {
                if (!f.isSynthetic()) {
                    Object fieldVal = null;
                    if (f.getAnnotation(Inject.class) != null) {
                        final Class<?> fType = f.getType();
                        if (fType == JobContext.class) {
                            fieldVal = jobContext;
                        } else if (fType == StepContext.class) {
                            //fieldVal may be null when StepContext was not stored in data map, as in job listeners
                            fieldVal = stepContext;
                        } else if (hasBatchProps) {
                            final BatchProperty batchPropertyAnn = f.getAnnotation(BatchProperty.class);
                            if (batchPropertyAnn != null) {
                                String propName = batchPropertyAnn.name();
                                if (propName.equals("")) {
                                    propName = f.getName();
                                }
                                final String sVal = batchProps.get(propName);
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

    protected void invokeAnnotatedLifecycleMethod(final Object obj, Class<?> cls, final Class<? extends Annotation> annCls) throws Exception{
        final List<Method> lifecycleMethods = new ArrayList<Method>();
        while (cls != null && cls != Object.class && cls.getPackage() != null && !cls.getPackage().getName().startsWith("jakarta.batch")) {
            final Method[] methods = cls.getDeclaredMethods();
            for (final Method m : methods) {
                if (m.getAnnotation(annCls) != null) {  //the lifecyle annotation is present
                    final int modifiers = m.getModifiers();
                    final String mName = m.getName();
                    if (Modifier.isPrivate(modifiers)) {
                        lifecycleMethods.add(m);
                    } else {
                        boolean alreadyAdded = false;
                        for (final Method lm : lifecycleMethods) {
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
        for(final Method m : lifecycleMethods) {
            if (WildFlySecurityManager.isChecking()) {
                AccessController.doPrivileged(new InvokeMethodPrivilegedExceptionAction(m, obj));
            } else {
                if (!m.isAccessible()) {
                    m.setAccessible(true);
                }
                m.invoke(obj);
            }
        }
    }

    private void doInjection(final Object obj, final Field field, final Object val) throws Exception {
        if (WildFlySecurityManager.isChecking()) {
            AccessController.doPrivileged(new SetFieldPrivilegedExceptionAction(field, obj, val));
        } else {
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            field.set(obj, val);
        }
    }

    private static class SetFieldPrivilegedExceptionAction implements PrivilegedExceptionAction<Void> {
        private final Field field;
        private final Object obj;
        private final Object val;

        public SetFieldPrivilegedExceptionAction(final Field field, final Object obj, final Object val) {
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

        public InvokeMethodPrivilegedExceptionAction(final Method method, final Object obj) {
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
