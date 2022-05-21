/*
 * Copyright (c) 2013-2018 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.creation;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.URI;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;
import javax.management.ObjectName;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.runtime.context.JobContext;
import jakarta.batch.runtime.context.StepContext;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.InjectionPoint;
import org.jberet._private.BatchLogger;
import org.jberet._private.BatchMessages;
import org.jberet.job.model.Properties;
import org.wildfly.security.manager.WildFlySecurityManager;

public class BatchBeanProducer {
    @Produces
    public JobContext getJobContext() {
        return ArtifactCreationContext.getCurrentArtifactCreationContext().jobContext;
    }

    @Produces
    public StepContext getStepContext() {
        return ArtifactCreationContext.getCurrentArtifactCreationContext().stepContext;
    }

    @Produces @BatchProperty
    public Integer getInt(final InjectionPoint injectionPoint) {
        Integer i = getProperty(injectionPoint);
        if (i == null) {
            Class<?> fType = ((Field) (injectionPoint.getMember())).getType();
            return fType == Integer.class ? null : 0;
        }
        return i;
    }

    @Produces @BatchProperty
    public int[] getIntArray(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public Integer[] getIntegerArray(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public Long getLong(final InjectionPoint injectionPoint) {
        Long l = getProperty(injectionPoint);
        if (l == null) {
            Class<?> fType = ((Field) (injectionPoint.getMember())).getType();
            return fType == Long.class ? null : 0L;
        }
        return l;
    }

    @Produces @BatchProperty
    public Long[] getBigLongArray(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public long[] getLongArray(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public Short getShort(final InjectionPoint injectionPoint) {
        Short sh = getProperty(injectionPoint);
        if (sh == null) {
            Class<?> fType = ((Field) (injectionPoint.getMember())).getType();
            return fType == Short.class ? null : (short) 0;
        }
        return sh;
    }

    @Produces @BatchProperty
    public Short[] getBigShortArray(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public short[] getShortArray(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public Byte getByte(final InjectionPoint injectionPoint) {
        Byte b = getProperty(injectionPoint);
        if (b == null) {
            Class<?> fType = ((Field) (injectionPoint.getMember())).getType();
            return fType == Byte.class ? null : (byte) 0;
        }
        return b;
    }

    @Produces @BatchProperty
    public Byte[] getBigByteArray(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public byte[] getByteArray(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public Character getCharacter(final InjectionPoint injectionPoint) {
        Character ch = getProperty(injectionPoint);
        if (ch == null) {
            Class<?> fType = ((Field) (injectionPoint.getMember())).getType();
            return fType == Character.class ? null : '\u0000';
        }
        return ch;
    }

    @Produces @BatchProperty
    public Character[] getCharacterArray(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public char[] getCharArray(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public Float getFloat(final InjectionPoint injectionPoint) {
        Float f = getProperty(injectionPoint);
        if (f == null) {
            Class<?> fType = ((Field) (injectionPoint.getMember())).getType();
            return fType == Float.class ? null : 0F;
        }
        return f;
    }

    @Produces @BatchProperty
    public Float[] getBigFloatArray(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public float[] getFloatArray(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public Double getDouble(final InjectionPoint injectionPoint) {
        Double d = getProperty(injectionPoint);
        if (d == null) {
            Class<?> fType = ((Field) (injectionPoint.getMember())).getType();
            return fType == Double.class ? null : 0D;
        }
        return d;
    }

    @Produces @BatchProperty
    public Double[] getBigDoubleArray(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public double[] getDoubleArray(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public Boolean getBoolean(final InjectionPoint injectionPoint) {
        Boolean b = getProperty(injectionPoint);
        if (b == null) {
            Class<?> fType = ((Field) (injectionPoint.getMember())).getType();
            return fType == Boolean.class ? null : Boolean.FALSE;
        }
        return b;
    }

    @Produces @BatchProperty
    public Boolean[] getBigBooleanArray(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public boolean[] getBooleanArray(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public String getString(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public String[] getStringArray(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public Date getDate(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public Date[] getDateArray(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public Class getClazz(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public Class[] getClazzArray(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public Inet4Address getInet4Address(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public Inet6Address getInet6Address(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public Map getMap(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public Map<String, String> getStringMap(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public Set getSet(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public Set<String> getStringSet(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public Logger getLogger(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public Pattern getPattern(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public ObjectName getObjectName(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public List getList(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public List<String> getStringList(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public BigInteger getBigInteger(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public BigInteger[] getBigIntegerArray(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public BigDecimal getBigDecimal(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public BigDecimal[] getBigDecimalArray(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public URL getURL(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public URL[] getURLArray(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public URI getURI(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public URI[] getURIArray(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public File getFile(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public File[] getFileArray(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public ZipFile[] getZipFileArray(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public JarFile getJarFile(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public JarFile[] getJarFileArray(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public StringBuilder getStringBuilder(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    @Produces @BatchProperty
    public StringBuffer getStringBuffer(final InjectionPoint injectionPoint) {
        return getProperty(injectionPoint);
    }

    private <T> T getProperty(final InjectionPoint injectionPoint) {
        final ArtifactCreationContext ac = ArtifactCreationContext.getCurrentArtifactCreationContext();
        final Properties properties = ac.properties;
        final Annotated annotated = injectionPoint.getAnnotated();
        final Member injectionTarget = injectionPoint.getMember();
        BatchProperty batchProperty;
        String propName;

        if (annotated == null) {
            final Set<Annotation> qualifiers = injectionPoint.getQualifiers();
            for (Annotation ann : qualifiers) {
                if (ann instanceof BatchProperty) {
                    batchProperty = (BatchProperty) ann;
                    propName = batchProperty.name();
                    if (propName.isEmpty()) {
                        throw BatchMessages.MESSAGES.batchPropertyNameMissing(injectionTarget);
                    }
                    return properties == null ? null : (T) properties.get(propName);
                }
            }
            return null;
        }

        batchProperty = annotated.getAnnotation(BatchProperty.class);
        propName = batchProperty.name();
        String rawVal;
        Class<?> paramOrFieldType;
        AnnotatedElement paramOfField;
        if (annotated instanceof AnnotatedParameter) {
            if (propName.isEmpty()) {
                throw BatchMessages.MESSAGES.batchPropertyNameMissing(injectionTarget);
            }
            rawVal = properties == null ? null : properties.get(propName);
            final Parameter param = ((AnnotatedParameter<?>) annotated).getJavaParameter();
            paramOrFieldType = param.getType();
            paramOfField = param;
        } else {
            final Field field = (Field) injectionTarget;
            paramOfField = field;
            if (propName.length() == 0) {
                propName = field.getName();
            }
            rawVal = properties == null ? null : properties.get(propName);

            if (rawVal == null) {
                try {
                    final Class<?> beanClass = injectionPoint.getBean().getBeanClass();

                    //best effort to get a default value
                    //no-arg constructor may not exist, in which case, we use null as its default value
                    //BatchLogger.LOGGER.infof("Injected batch property %s is not defined in job xml, will attempt to use the default value in class %s", propName, beanClass);
                    final Object o = beanClass.getDeclaredConstructor().newInstance();
                    final Object fieldVal;

                    if (WildFlySecurityManager.isChecking()) {
                        fieldVal = AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                            @Override
                            public Object run() throws Exception {
                                if (!field.isAccessible()) {
                                    field.setAccessible(true);
                                }
                                return field.get(o);
                            }
                        });
                    } else {
                        if (!field.isAccessible()) {
                            field.setAccessible(true);
                        }
                        fieldVal = field.get(o);
                    }
                    return (T) fieldVal;
                } catch (final Exception e) {
                    BatchLogger.LOGGER.tracef(e, "Failed to get the default value for undefined batch property %s, will use null.", propName);
                    return null;
                }
            }
            paramOrFieldType = field.getType();
        }

        if (rawVal == null || rawVal.isEmpty()) {
            return null;
        }
        return paramOrFieldType.isAssignableFrom(String.class) ? (T) rawVal :
                (T) ValueConverter.convertInjectionValue(rawVal, paramOrFieldType, paramOfField, ac.jobContext.getClassLoader());
    }
}
