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
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.WeakHashMap;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import static org.jberet._private.BatchMessages.MESSAGES;

/**
 * Converts a property string value to the target field type.  Supported field types for batch property injection:
 * <ul>
 *     <li>String</li>
 *     <li>StringBuffer</li>
 *     <li>StringBuilder</li>
 *     <li>Object</li>
 *     <li>Serializable</li>
 *     <li>CharSequence</li>
 *     <li>Comparable&lt;String&gt;</li>
 *     <li>int</li>
 *     <li>Integer</li>
 *     <li>long</li>
 *     <li>Long</li>
 *     <li>double</li>
 *     <li>Double</li>
 *     <li>boolean</li>
 *     <li>Boolean</li>
 *     <li>char</li>
 *     <li>Character</li>
 *     <li>float</li>
 *     <li>Float</li>
 *     <li>byte</li>
 *     <li>Byte</li>
 *     <li>short</li>
 *     <li>Short</li>
 *     <li>BigInteger</li>
 *     <li>BigDecimal</li>
 *     <li>java.util.Date</li>
 *     <li>Class</li>
 *     <li>Class&lt;&#63;&gt;</li>
 *     <li>any Enum</li>
 *     <li>java.io.File</li>
 *     <li>java.util.zip.ZipFile</li>
 *     <li>java.util.jar.JarFile</li>
 *     <li>URL</li>
 *     <li>URI</li>
 *     <li>InetAddress</li>
 *     <li>Inet4Address</li>
 *     <li>Inet6Address</li>
 *     <li>java.util.logging.Logger</li>
 *     <li>java.util.regex.Pattern</li>
 *     <li>javax.management.ObjectName</li>
 *     <br/>
 *     <li>array of any of the above single-valued type, e.g., int[], Object[], Integer[], String[], Date[], TimeUnit[], etc</li>
 *     <br/>
 *     <li>java.util.Collection</li>
 *     <li>List</li>
 *     <li>ArrayList</li>
 *     <li>LinkedList</li>
 *     <li>Vector</li>
 *     <li>Set</li>
 *     <li>HashSet</li>
 *     <li>SortedSet</li>
 *     <li>TreeSet</li>
 *     <li>LinkedHashSet</li>
 *     <li>java.util.Properties</li>
 *     <li>Map</li>
 *     <li>HashMap</li>
 *     <li>Hashtable</li>
 *     <li>IdentityHashMap</li>
 *     <li>LinkedHashMap</li>
 *     <li>SortedMap</li>
 *     <li>TreeMap</li>
 *     <li>WeakHashMap</li>
 * </ul>
 * <br/>
 * Common generics collections are supported.  Wildcard is treated the same as String.  for example:<ul>
 *     <li>Collection&lt;&#63;&gt;</li>
 *     <li>List&lt;String&gt;</li>
 *     <li>Set&lt;Date&gt;</li>
 *     <li>Map&lt;&#63;, &#63;&gt;</li>
 *     <li>Map&lt;String, &#63;&gt;</li>
 *     <li>LinkedList&lt;TimeUnit&gt;</li>
 *     <li>ArrayList&lt;Integer&gt;</li>
 * </ul>
 */
public final class ValueConverter {
    private static final String delimiter = ",";

    // order from longest to shortest to make sure input date strings are not truncated by shorter format styles.
    private static final int[] dateFormatCodes = {DateFormat.FULL, DateFormat.LONG, DateFormat.MEDIUM, DateFormat.SHORT};

    public static Object convertFieldValue(final String rawValue, final Class<?> t, final Field f, final ClassLoader classLoader) {
        final Object result = convertSingleValue(rawValue, t, f, classLoader);
        if (result != null) {
            return result;
        }
        final String v = rawValue.trim();
        Class<?> elementValueType = null;

        if (t.isArray()) {
            elementValueType = t.getComponentType();
            if (elementValueType.isPrimitive()) {
                return parsePrimitiveArray(v, elementValueType, f);
            } else {
                final List tempList = parseList(v, new ArrayList(), elementValueType, f, classLoader);
                final Object[] tempArray = (Object[]) Array.newInstance(elementValueType, tempList.size());
                return tempList.toArray(tempArray);
            }
        }

        if (t == java.util.Properties.class) {
            final java.util.Properties p = new java.util.Properties();
            return parseMap(v, p, String.class, f, classLoader);
        }

        final Type genericType = f.getGenericType();
        if (genericType instanceof ParameterizedType) {
            final ParameterizedType pt = (ParameterizedType) genericType;
            final Type[] actualTypeArguments = pt.getActualTypeArguments();
            final Class<?>[] elementTypes = new Class<?>[actualTypeArguments.length];
            for (int i = 0; i < actualTypeArguments.length; i++) {
                if (actualTypeArguments[i] instanceof Class) {
                    elementTypes[i] = (Class<?>) actualTypeArguments[i];
                }
                // can be ? or ? extends SomeType, which is sun.reflect.generics.reflectiveObjects.WildcardTypeImpl,
                // not Class type.
            }
            switch (elementTypes.length) {
                case 1:
                    elementValueType = elementTypes[0];
                    break;
                case 2:
                    elementValueType = elementTypes[1];
                    break;
            }
        }
        //can be List or Set (raw collection) so there is no elementValueType, set to String.class
        if (elementValueType == null) {
            elementValueType = String.class;
        }

        if (List.class.isAssignableFrom(t) || t == Collection.class) {
            final List l;
            if (t == List.class || t == ArrayList.class || t == Collection.class) {
                l = new ArrayList();
            } else if (t == LinkedList.class) {
                l = new LinkedList();
            } else if (t == Vector.class) {
                l = new Vector();
            } else {
                throw MESSAGES.unsupportedFieldType(v, f, t);
            }
            return parseList(v, l, elementValueType, f, classLoader);
        }
        if (Map.class.isAssignableFrom(t)) {
            final Map<String, String> m;
            if (t == Map.class || t == HashMap.class) {
                m = new HashMap();
            } else if (t == LinkedHashMap.class) {
                m = new LinkedHashMap();
            } else if (t == IdentityHashMap.class) {
                m = new IdentityHashMap();
            } else if (t == Hashtable.class) {
                m = new Hashtable();
            } else if (t == TreeMap.class || t == SortedMap.class) {
                m = new TreeMap();
            } else if (t == WeakHashMap.class) {
                m = new WeakHashMap();
            } else {
                throw MESSAGES.unsupportedFieldType(v, f, t);
            }
            return parseMap(v, m, elementValueType, f, classLoader);
        }
        if (Set.class.isAssignableFrom(t)) {
            final Set set;
            if (t == Set.class || t == HashSet.class) {
                set = new HashSet();
            } else if (t == SortedSet.class || t == TreeSet.class) {
                set = new TreeSet();
            } else if (t == LinkedHashSet.class) {
                set = new LinkedHashSet();
            } else {
                throw MESSAGES.unsupportedFieldType(v, f, t);
            }
            set.addAll(parseList(v, new ArrayList(), elementValueType, f, classLoader));
            return set;
        }

        throw MESSAGES.unsupportedFieldType(v, f, t);
    }

    private static Object convertSingleValue(final String rawValue, final Class<?> t, final Field f, final ClassLoader classLoader) {
        final String v = rawValue.trim();
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
            return rawValue.charAt(0);
        }
        if (t == byte.class || t == Byte.class) {
            return Byte.valueOf(v);
        }
        if (t == short.class || t == Short.class) {
            return Short.valueOf(v);
        }
        if (t == File.class) {
            return new File(v);
        }
        if (t == ZipFile.class) {
            try {
                return new ZipFile(v);
            } catch (IOException e) {
                throw MESSAGES.failToInjectProperty(e, v, f);
            }
        }
        if (t == JarFile.class) {
            try {
                return new JarFile(v);
            } catch (IOException e) {
                throw MESSAGES.failToInjectProperty(e, v, f);
            }
        }
        if (t == URL.class) {
            try {
                return new URI(v).toURL();
            } catch (Exception e) {
                throw MESSAGES.failToInjectProperty(e, v, f);
            }
        }
        if (t == URI.class) {
            try {
                return new URI(v);
            } catch (Exception e) {
                throw MESSAGES.failToInjectProperty(e, v, f);
            }
        }
        if (t == InetAddress.class || t == Inet4Address.class || t == Inet6Address.class) {
            try {
                return InetAddress.getByName(v);
            } catch (Exception e) {
                throw MESSAGES.failToInjectProperty(e, v, f);
            }
        }
        if (t == BigDecimal.class) {
            return new BigDecimal(v);
        }
        if (t == BigInteger.class) {
            return new BigInteger(v);
        }
        if (t == java.util.Date.class) {
            return parseDate(v, f);
        }
        if (t == Class.class) {
            try {
                return Class.forName(v, false, classLoader);
            } catch (ClassNotFoundException e) {
                throw MESSAGES.failToInjectProperty(e, v, f);
            }
        }
        if (t.isEnum()) {
            return Enum.valueOf((Class<? extends Enum>) t, v);
        }
        if (t == Pattern.class) {
            return Pattern.compile(v);
        }
        if (t == java.util.logging.Logger.class) {
            return Logger.getLogger(v);
        }
        if (t == StringBuilder.class) {
            return new StringBuilder(rawValue);
        }
        if (t == StringBuffer.class) {
            return new StringBuffer(rawValue);
        }
        if (t == ObjectName.class) {
            try {
                return new ObjectName(v);
            } catch (MalformedObjectNameException e) {
                throw MESSAGES.failToInjectProperty(e, v, f);
            }
        }
        return null;
    }

    private static Object parsePrimitiveArray(final String v, final Class<?> primitiveType, final Field f) {
        final StringTokenizer st = new StringTokenizer(v, delimiter);
        final int count = st.countTokens();
        final String[] sVal = new String[count];
        for(int i = 0; i < count; i++) {
            final String s = st.nextToken().trim();
            sVal[i] = s;
        }
        if (primitiveType == int.class) {
            final int[] result = new int[count];
            for (int i = 0; i < count; i++) {
                result[i] = Integer.parseInt(sVal[i]);
            }
            return result;
        }
        if (primitiveType == long.class) {
            final long[] result = new long[count];
            for (int i = 0; i < count; i++) {
                result[i] = Long.parseLong(sVal[i]);
            }
            return result;
        }
        if (primitiveType == double.class) {
            final double[] result = new double[count];
            for (int i = 0; i < count; i++) {
                result[i] = Double.parseDouble(sVal[i]);
            }
            return result;
        }
        if (primitiveType == boolean.class) {
            final boolean[] result = new boolean[count];
            for (int i = 0; i < count; i++) {
                result[i] = Boolean.parseBoolean(sVal[i]);
            }
            return result;                }
        if (primitiveType == float.class) {
            final float[] result = new float[count];
            for (int i = 0; i < count; i++) {
                result[i] = Float.parseFloat(sVal[i]);
            }
            return result;
        }
        if (primitiveType == char.class) {
            final char[] result = new char[count];
            for (int i = 0; i < count; i++) {
                result[i] = sVal[i].charAt(0);
            }
            return result;
        }
        if (primitiveType == byte.class) {
            final byte[] result = new byte[count];
            for (int i = 0; i < count; i++) {
                result[i] = Byte.parseByte(sVal[i]);
            }
            return result;                }
        if (primitiveType == short.class) {
            final short[] result = new short[count];
            for (int i = 0; i < count; i++) {
                result[i] = Short.parseShort(sVal[i]);
            }
            return result;
        }
        throw MESSAGES.failToInjectProperty(null, v, f);
    }

    private static List parseList(final String v, final List l, final Class<?> elementValueType, final Field f, final ClassLoader classLoader) {
        final StringTokenizer st = new StringTokenizer(v, delimiter);
        while (st.hasMoreTokens()) {
            final String s = st.nextToken().trim();
            if (elementValueType.isAssignableFrom(String.class)) {
                l.add(s.equals("null") ? null : s);
            } else {
                l.add(convertSingleValue(s, elementValueType, f, classLoader));
            }
        }
        return l;
    }

    private static Map parseMap(final String v, final Map map, final Class<?> elementValueType, final Field f, final ClassLoader classLoader) {
        final StringTokenizer st = new StringTokenizer(v, delimiter);
        while (st.hasMoreTokens()) {
            final String pair = st.nextToken().trim();
            final int i = pair.indexOf('=');
            final String key;
            final String value;
            if (i > 0) {
                key = pair.substring(0, i).trim();
                value = pair.substring(i + 1).trim();
            } else if (i < 0) {
                key = pair;
                value = null;
            } else {
                throw MESSAGES.failToInjectProperty(null, v, f);
            }
            if (elementValueType == String.class) {
                map.put(key, value);
            } else {
                map.put(key, convertSingleValue(value, elementValueType, f, classLoader));
            }
        }
        return map;
    }

    /**
     * Supported date formats are based on the pre-defined styles in java.text.DateFormat.
     * <ul>
     * <li>FULL date FULL time</li>
     * <li>LONG date LONG time</li>
     * <li>MEDIUM date MEDIUM time</li>
     * <li>SHORT date SHORT time</li>
     * <li>FULL date</li>
     * <li>LONG date</li>
     * <li>MEDIUM date</li>
     * <li>SHORT date</li>
     * </ul><br/>
     * Examples in en_US locale:
     *<ul>
     * <li>Saturday, April 12, 1952 7:03:47 AM PDT</li>
     * <li>June 09, 2013 7:03:47 AM PDT</li>
     * <li>Jun 09, 2013 7:03:47 AM</li>
     * <li>05/09/2013 7:03 AM</li>
     * <li>05/09/13 12:00 AM</li>
     * <li>Saturday, April 12, 1952</li>
     * <li>June 09, 2013</li>
     * <li>Jun 09, 2013</li>
     * <li>05/09/2013</li>
     * <li>05/09/13</li>
     * </ul>
     * @param v input date string
     * @param f the field in the batch artifact (the injection target field)
     * @return a java.util.Date object parsed from the input string v
     */
    private static Date parseDate(final String v, final Field f) {
        DateFormat df;
        for (final int p : dateFormatCodes) {
            df = DateFormat.getDateTimeInstance(p, p);
            df.setLenient(false);
            try {
                return df.parse(v);
            } catch (ParseException e) {
                //ignore
            }
        }
        for (final int p : dateFormatCodes) {
            df = DateFormat.getDateInstance(p);
            df.setLenient(false);
            try {
                return df.parse(v);
            } catch (ParseException e) {
                //ignore
            }
        }
        throw MESSAGES.failToInjectProperty(null, v, f);
    }

}
