/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
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

import java.io.File;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;
import javax.batch.api.BatchProperty;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.management.ObjectName;

import org.jberet.job.model.Properties;

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
        final BatchProperty batchProperty = injectionPoint.getAnnotated().getAnnotation(BatchProperty.class);
        String propName = batchProperty.name();
        final Field field = (Field) injectionPoint.getMember();

        if (propName.length() == 0) {
            propName = field.getName();
        }
        final ArtifactCreationContext ac = ArtifactCreationContext.getCurrentArtifactCreationContext();
        final Properties properties = ac.properties;
        if (properties != null) {
            final String rawVal = properties.get(propName);
            if (rawVal == null || rawVal.isEmpty()) {
                return null;
            }
            final Class<?> fieldType = field.getType();
            return fieldType.isAssignableFrom(String.class) ? (T) rawVal :
                    (T) ValueConverter.convertFieldValue(rawVal, fieldType, field, ac.jobContext.getClassLoader());
        }
        return null;
    }
}
