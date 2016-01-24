/*
 * Copyright (c) 2014-2016 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.samples.wildfly.batchproperty;

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
import javax.batch.api.AbstractBatchlet;
import javax.batch.api.BatchProperty;
import javax.inject.Inject;
import javax.inject.Named;
import javax.management.ObjectName;

/**
 * A sample batchlet that contains {@code @BatchProperty} field injections of various field types, e.g., String, int,
 * boolean, List, Set, Map, String[], int[], Class, java.util.Date, etc.
 *
 * @author Cheng Fang
 */
@Named
public class BatchPropertyBatchlet extends AbstractBatchlet {
    @Inject
    @BatchProperty(name = "int")
    int anInt;

    @Inject
    @BatchProperty(name = "int")
    long aLong;

    @Inject
    @BatchProperty(name = "list")
    int[] ints;

    @Inject
    @BatchProperty(name = "list")
    long[] longs;

    @Inject
    @BatchProperty(name = "list")
    char[] chars;

    @Inject
    @BatchProperty(name = "booleans")
    boolean[] booleans;

    @Inject
    @BatchProperty(name = "list")
    String[] listStringArray;

    @Inject
    @BatchProperty(name = "list.date")
    Date[] listDateArray;

    @Inject
    @BatchProperty(name = "class")
    Class cls;

    @Inject
    @BatchProperty(name = "class")
    Class[] clss;

    @Inject
    @BatchProperty(name = "inet4.address")
    Inet4Address inet4Address;

    @Inject
    @BatchProperty(name = "inet6.address")
    Inet6Address inet6Address;

    @Inject
    @BatchProperty(name = "map")
    Map<String, String> map;

    @Inject
    @BatchProperty(name = "set")
    Set<String> set;

    @Inject
    @BatchProperty(name = "logger")
    Logger logger;

    @Inject
    @BatchProperty(name = "pattern")
    Pattern pattern;

    @Inject
    @BatchProperty(name = "object.name")
    ObjectName objectName;

    @Inject
    @BatchProperty(name = "list")
    private List<String> list;

    @Inject
    @BatchProperty(name = "big.integer")
    private BigInteger bigInteger;

    @Inject
    @BatchProperty(name = "big.decimal")
    private BigDecimal bigDecimal;

    @Inject
    @BatchProperty(name = "url")
    private URL url;

    @Inject
    @BatchProperty(name = "uri")
    private URI uri;

    @Inject
    @BatchProperty(name = "file")
    private File file;

    @Inject
    @BatchProperty(name = "jar.files")
    JarFile[] jarFiles;

    @Override
    public String process() throws Exception {
        final Field[] declaredFields = this.getClass().getDeclaredFields();
        for (final Field field : declaredFields) {
            if (field.getAnnotation(BatchProperty.class) != null) {
                final Class<?> fieldType = field.getType();
                final Object fieldValue = field.get(this);
                System.out.printf("Field injection: %s %s = %s;%n", fieldType, field.getName(), fieldValue);
            }
        }

        return null;
    }
}
