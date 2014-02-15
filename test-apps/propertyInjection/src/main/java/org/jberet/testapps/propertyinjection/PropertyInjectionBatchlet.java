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

package org.jberet.testapps.propertyinjection;

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
import javax.inject.Inject;
import javax.inject.Named;
import javax.management.ObjectName;

import org.jberet.testapps.common.BatchletNoNamed;

@Named
public class PropertyInjectionBatchlet extends BatchletNoNamed {
    public enum Color {RED, WHITE, YELLOW}

    @Inject @BatchProperty(name = "int")
    int anInt;

    @Inject @BatchProperty(name = "int")
    long aLong;

    @Inject @BatchProperty(name = "int")
    short aShort;

    @Inject @BatchProperty(name = "int")
    byte aByte;

    @Inject @BatchProperty(name = "int")
    char aChar;

    @Inject @BatchProperty(name = "int")
    double aDouble;

    @Inject @BatchProperty(name = "int")
    float aFloat;

    @Inject @BatchProperty(name = "boolean")
    boolean aBoolean;


    @Inject @BatchProperty(name = "list")
    int[] ints;

    @Inject @BatchProperty(name = "list")
    long[] longs;

    @Inject @BatchProperty(name = "list")
    short[] shorts;

    @Inject @BatchProperty(name = "list")
    byte[] bytes;

    @Inject @BatchProperty(name = "list")
    char[] chars;

    @Inject @BatchProperty(name = "list")
    double[] doubles;

    @Inject @BatchProperty(name = "list")
    float[] floats;

    @Inject @BatchProperty(name = "booleans")
    boolean[] booleans;


    @Inject @BatchProperty(name = "list")
    Integer[] intsWrapper;

    @Inject @BatchProperty(name = "list")
    Long[] longsWrapper;

    @Inject @BatchProperty(name = "list")
    Short[] shortsWrapper;

    @Inject @BatchProperty(name = "list")
    Byte[] bytesWrapper;

    @Inject @BatchProperty(name = "list")
    Character[] charsWrapper;

    @Inject @BatchProperty(name = "list")
    Double[] doublesWrapper;

    @Inject @BatchProperty(name = "list")
    Float[] floatsWrapper;

    @Inject @BatchProperty(name = "booleans")
    Boolean[] booleansWrapper;


    @Inject @BatchProperty(name = "list")
    String[] listStringArray;

    @Inject @BatchProperty(name = "list.date")
    Date[] listDateArray;


    @Inject @BatchProperty(name = "class")
    Class cls;

    @Inject @BatchProperty(name = "class")
    Class[] clss;

    @Inject @BatchProperty(name = "stringArrayClass")
    Class stringArrayClass;

    @Inject @BatchProperty(name = "inet4.address")
    Inet4Address inet4Address;

    @Inject @BatchProperty(name = "inet6.address")
    Inet6Address inet6Address;




    @Inject @BatchProperty(name = "map")
    Map<String, String> map;

    @Inject @BatchProperty(name = "map")
    Map mapRaw;

    @Inject @BatchProperty(name = "map")
    Map<?, ?> mapWild;

    @Inject @BatchProperty(name = "map")
    Map<String, ?> mapWildValue;



    //@Inject @BatchProperty(name = "map")
    //Properties properties;


    @Inject @BatchProperty(name = "set")
    Set<String> set;

    @Inject @BatchProperty(name = "set")
    Set<?> setWild;

    @Inject @BatchProperty(name = "set")
    Set rawSet;

    //@Inject @BatchProperty(name = "set")
    //Set<Float> setFloat;


    @Inject @BatchProperty(name = "logger")
    Logger logger;

    @Inject @BatchProperty(name = "pattern")
    Pattern pattern;

    @Inject @BatchProperty(name = "object.name")
    ObjectName objectName;

    @Inject @BatchProperty(name = "date.time.short")
    private Date dateTimeShort;

    @Inject @BatchProperty(name = "date.time.short2")
    private Date dateTimeShort2;

    @Inject @BatchProperty(name = "date.time.medium")
    private Date dateTimeMedium;

    @Inject @BatchProperty(name = "date.time.long")
    private Date dateTimeLong;

    @Inject @BatchProperty(name = "date.time.full")
    private Date dateTimeFull;


    @Inject @BatchProperty(name = "date.short")
    private Date dateShort;

    @Inject @BatchProperty(name = "date.short2")
    private Date dateShort2;

    @Inject @BatchProperty(name = "date.medium")
    private Date dateMedium;

    @Inject @BatchProperty(name = "date.long")
    private Date dateLong;

    @Inject @BatchProperty(name = "date.full")
    private Date dateFull;


    @Inject @BatchProperty(name = "list")
    private List<String> list;

    @Inject @BatchProperty(name = "list")
    List rawList;

    @Inject @BatchProperty(name = "list")
    List<?> listWild;


    //@Inject @BatchProperty(name = "list.date")
    //List<Date> listDate;
    //
    //@Inject @BatchProperty(name = "list.date")
    //List listDateString;
    //
    //@Inject @BatchProperty(name = "list.enum")
    //List<Color> listColor;




    @Inject @BatchProperty(name = "big.integer")
    private BigInteger bigInteger;

    @Inject @BatchProperty(name = "big.decimal")
    private BigDecimal bigDecimal;

    @Inject @BatchProperty(name = "url")
    private URL url;

    @Inject @BatchProperty(name = "uri")
    private URI uri;

    @Inject @BatchProperty(name = "file")
    private File file;

    @Inject @BatchProperty(name = "jar.files")
    JarFile[] jarFiles;

    @Inject @BatchProperty(name = "jar.files")
    ZipFile[] zipFiles;



    //@Inject @BatchProperty(name = "jar.files")
    //List<JarFile> jarFileList;
    //
    //@Inject @BatchProperty(name = "jar.files")
    //List<ZipFile> zipFileList;




    @Inject @BatchProperty(name = "jar.file")
    JarFile jarFile;

    //@Inject @BatchProperty(name = "jar.file")
    //ZipFile zipFile;

    @Inject @BatchProperty(name = "string")
    StringBuilder stringBuilder;

    @Inject @BatchProperty(name = "string")
    StringBuffer stringBuffer;



    @Inject @BatchProperty(name = "include.not.defined")
    String includeNotDefined;

    @Inject @BatchProperty(name = "include.not.defined.2")
    String includeNotDefined2;

    @Inject @BatchProperty(name = "include.not.defined.3")
    String includeNotDefined3;

    @Inject @BatchProperty(name = "not.defined")
    String notDefined;

    @Override
    public String process() throws Exception {
        final Field[] declaredFields = this.getClass().getDeclaredFields();
        for (final Field field :declaredFields) {
            if (field.getAnnotation(BatchProperty.class) != null) {
                final Class<?> fieldType = field.getType();
                final Object fieldValue = field.get(this);
                System.out.printf("Field injection: %s %s = %s;%n", fieldType, field.getName(), fieldValue);
            }
        }

        return includeNotDefined + " " + includeNotDefined2 + " " + includeNotDefined3 + " " + notDefined;
    }
}
