/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import java.io.File;
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
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import static org.jberet.util.BatchLogger.LOGGER;

public final class ValueConverter {
    private static final String delimiter = ",";

    // order from longest to shortest to make sure input date strings are not truncated by shorter format stylers.
    private static final int[] dateFormatCodes = {DateFormat.FULL, DateFormat.LONG, DateFormat.MEDIUM, DateFormat.SHORT};

    public static Object convertFieldValue(String rawValue, Class<?> t, Field f, ClassLoader classLoader) {
        Object result = convertSingleValue(rawValue, t, f, classLoader);
        if (result != null) {
            return result;
        }
        String v = rawValue.trim();
        Class<?> elementValueType = null;

        if (t.isArray()) {
            elementValueType = t.getComponentType();
            if (elementValueType.isPrimitive()) {
                return parsePrimitiveArray(v, elementValueType, f);
            } else {
                List tempList = parseList(v, new ArrayList(), elementValueType, f, classLoader);
                Object[] tempArray = (Object[]) Array.newInstance(elementValueType, tempList.size());
                return tempList.toArray(tempArray);
            }
        }

        if (t == java.util.Properties.class) {
            java.util.Properties p = new java.util.Properties();
            return parseMap(v, p, String.class, f, classLoader);
        }

        Type genericType = f.getGenericType();
        if (genericType instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) genericType;
            Type[] actualTypeArguments = pt.getActualTypeArguments();
            Class<?>[] elementTypes = new Class<?>[actualTypeArguments.length];
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

        if (List.class.isAssignableFrom(t)) {
            List l;
            if (t == List.class || t == ArrayList.class) {
                l = new ArrayList();
            } else if (t == LinkedList.class) {
                l = new LinkedList();
            } else if (t == Vector.class) {
                l = new Vector();
            } else {
                throw LOGGER.unsupportedFieldType(v, f, t);
            }
            return parseList(v, l, elementValueType, f, classLoader);
        }
        if (Map.class.isAssignableFrom(t)) {
            Map<String, String> m;
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
                throw LOGGER.unsupportedFieldType(v, f, t);
            }
            return parseMap(v, m, elementValueType, f, classLoader);
        }
        if (Set.class.isAssignableFrom(t)) {
            Set set;
            if (t == Set.class || t == HashSet.class) {
                set = new HashSet();
            } else if (t == SortedSet.class || t == TreeSet.class) {
                set = new TreeSet();
            } else if (t == LinkedHashSet.class) {
                set = new LinkedHashSet();
            } else {
                throw LOGGER.unsupportedFieldType(v, f, t);
            }
            set.addAll(parseList(v, new ArrayList(), elementValueType, f, classLoader));
            return set;
        }

        throw LOGGER.unsupportedFieldType(v, f, t);
    }

    private static Object convertSingleValue(String rawValue, Class<?> t, Field f, ClassLoader classLoader) {
        String v = rawValue.trim();
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
        if (t == URL.class) {
            try {
                return new URI(v).toURL();
            } catch (Exception e) {
                throw LOGGER.failToInjectProperty(e, v, f);
            }
        }
        if (t == URI.class) {
            try {
                return new URI(v);
            } catch (Exception e) {
                throw LOGGER.failToInjectProperty(e, v, f);
            }
        }
        if (t == InetAddress.class || t == Inet4Address.class || t == Inet6Address.class) {
            try {
                return InetAddress.getByName(v);
            } catch (Exception e) {
                throw LOGGER.failToInjectProperty(e, v, f);
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
                return classLoader.loadClass(v);
            } catch (ClassNotFoundException e) {
                throw LOGGER.failToInjectProperty(e, v, f);
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
                throw LOGGER.failToInjectProperty(e, v, f);
            }
        }
        return null;
    }

    private static Object parsePrimitiveArray(String v, Class<?> primitiveType, Field f) {
        StringTokenizer st = new StringTokenizer(v, delimiter);
        int count = st.countTokens();
        String[] sVal = new String[count];
        for(int i = 0; i < count; i++) {
            String s = st.nextToken().trim();
            sVal[i] = s;
        }
        if (primitiveType == int.class) {
            int[] result = new int[count];
            for (int i = 0; i < count; i++) {
                result[i] = Integer.parseInt(sVal[i]);
            }
            return result;
        }
        if (primitiveType == long.class) {
            long[] result = new long[count];
            for (int i = 0; i < count; i++) {
                result[i] = Long.parseLong(sVal[i]);
            }
            return result;
        }
        if (primitiveType == double.class) {
            double[] result = new double[count];
            for (int i = 0; i < count; i++) {
                result[i] = Double.parseDouble(sVal[i]);
            }
            return result;
        }
        if (primitiveType == boolean.class) {
            boolean[] result = new boolean[count];
            for (int i = 0; i < count; i++) {
                result[i] = Boolean.parseBoolean(sVal[i]);
            }
            return result;                }
        if (primitiveType == float.class) {
            float[] result = new float[count];
            for (int i = 0; i < count; i++) {
                result[i] = Float.parseFloat(sVal[i]);
            }
            return result;
        }
        if (primitiveType == char.class) {
            char[] result = new char[count];
            for (int i = 0; i < count; i++) {
                result[i] = v.charAt(0);
            }
            return result;
        }
        if (primitiveType == byte.class) {
            byte[] result = new byte[count];
            for (int i = 0; i < count; i++) {
                result[i] = Byte.parseByte(sVal[i]);
            }
            return result;                }
        if (primitiveType == short.class) {
            short[] result = new short[count];
            for (int i = 0; i < count; i++) {
                result[i] = Short.parseShort(sVal[i]);
            }
            return result;
        }
        throw LOGGER.failToInjectProperty(null, v, f);
    }

    private static List parseList(String v, List l, Class<?> elementValueType, Field f, ClassLoader classLoader) {
        StringTokenizer st = new StringTokenizer(v, delimiter);
        while (st.hasMoreTokens()) {
            String s = st.nextToken().trim();
            if (elementValueType == String.class) {
                l.add(s);
            } else {
                l.add(convertSingleValue(s, elementValueType, f, classLoader));
            }
        }
        return l;
    }

    private static Map parseMap(String v, Map map, Class<?> elementValueType, Field f, ClassLoader classLoader) {
        StringTokenizer st = new StringTokenizer(v, delimiter);
        while (st.hasMoreTokens()) {
            String pair = st.nextToken().trim();
            int i = pair.indexOf('=');
            String key, value;
            if (i > 0) {
                key = pair.substring(0, i).trim();
                value = pair.substring(i + 1).trim();
            } else if (i < 0) {
                key = pair;
                value = null;
            } else {
                throw LOGGER.failToInjectProperty(null, v, f);
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
    private static Date parseDate(String v, Field f) {
        DateFormat df;
        for (int p : dateFormatCodes) {
            df = DateFormat.getDateTimeInstance(p, p);
            df.setLenient(false);
            try {
                return df.parse(v);
            } catch (ParseException e) {
                //ignore
            }
        }
        for (int p : dateFormatCodes) {
            df = DateFormat.getDateInstance(p);
            df.setLenient(false);
            try {
                return df.parse(v);
            } catch (ParseException e) {
                //ignore
            }
        }
        throw LOGGER.failToInjectProperty(null, v, f);
    }

}
