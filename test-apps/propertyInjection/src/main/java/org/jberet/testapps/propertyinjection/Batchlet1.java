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

package org.jberet.testapps.propertyinjection;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
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
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.WeakHashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.batch.api.BatchProperty;
import javax.inject.Inject;
import javax.inject.Named;
import javax.management.ObjectName;

import org.jberet.testapps.common.Batchlet0;

@Named
public class Batchlet1 extends Batchlet0 {
    public enum Color {RED, WHITE, YELLOW}

    @Inject @BatchProperty(name = "int")
    int intNumber;

    @Inject @BatchProperty(name = "class")
    Class cls;

    @Inject @BatchProperty(name = "color")
    Color color;

    @Inject @BatchProperty(name = "inet.address")
    InetAddress inetAddress;

    @Inject @BatchProperty(name = "inet4.address")
    Inet4Address inet4Address;

    @Inject @BatchProperty(name = "inet6.address")
    Inet6Address inet6Address;

    @Inject @BatchProperty(name = "map")
    Map<String, String> mapComma;

    @Inject @BatchProperty(name = "map")
    HashMap<String, String> hashMapComma;

    @Inject @BatchProperty(name = "map")
    Hashtable<String, String> hashtableComma;

    @Inject @BatchProperty(name = "map")
    IdentityHashMap<String, String> identityHashMapComma;

    @Inject @BatchProperty(name = "map")
    LinkedHashMap<String, String> linkedHashMapComma;

    @Inject @BatchProperty(name = "map")
    Properties properties;

    @Inject @BatchProperty(name = "map")
    SortedMap<String, String> sortedMap;

    @Inject @BatchProperty(name = "map")
    TreeMap<String, String> treeMap;

    @Inject @BatchProperty(name = "map")
    WeakHashMap<String, String> weakHashMap;

    @Inject @BatchProperty(name = "set")
    Set<String> set;

    @Inject @BatchProperty(name = "set")
    LinkedHashSet<String> linkedHashSet;

    @Inject @BatchProperty(name = "set")
    HashSet<String> hashSet;

    @Inject @BatchProperty(name = "set")
    TreeSet<String> treeSet;

    @Inject @BatchProperty(name = "set")
    SortedSet<String> sortedSet;

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
    private List<String> listComma;

    @Inject @BatchProperty(name = "list")
    private ArrayList<String> arrayListComma;

    @Inject @BatchProperty(name = "list")
    private LinkedList<String> linkedListComma;

    @Inject @BatchProperty(name = "list")
    private Vector<String> vectorListComma;

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

    @Inject @BatchProperty(name = "string")
    StringBuilder stringBuilder;

    @Inject @BatchProperty(name = "string")
    StringBuffer stringBuffer;

    @Override
    public String process() throws Exception {
        String result = super.process();

        System.out.printf("cls: %s%n", cls);
        System.out.printf("enum color: %s%n", color);
        System.out.printf("inet.address: %s%n", inetAddress);
        System.out.printf("inet4.address: %s%n", inet4Address);
        System.out.printf("inet6.address: %s%n", inet6Address);

        System.out.printf("date.time.short:  %s%n", dateTimeShort);
        System.out.printf("date.time.short2: %s%n", dateTimeShort2);
        System.out.printf("date.time.medium: %s%n", dateTimeMedium);
        System.out.printf("date.time.long:   %s%n", dateTimeLong);
        System.out.printf("date.time.full:   %s%n", dateTimeFull);

        System.out.printf("date.short:  %s%n", dateShort);
        System.out.printf("date.short2: %s%n", dateShort2);
        System.out.printf("date.medium: %s%n", dateMedium);
        System.out.printf("date.long:   %s%n", dateLong);
        System.out.printf("date.full:   %s%n", dateFull);

        System.out.printf("list:     %s%n", listComma);
        System.out.printf("list(AL): %s%n", arrayListComma);
        System.out.printf("list(LL): %s%n", linkedListComma);
        System.out.printf("list(V):  %s%n", vectorListComma);

        System.out.printf("map: %s%n", mapComma);
        System.out.printf("map(HM): %s%n", hashMapComma);
        System.out.printf("map(HT): %s%n", hashtableComma);
        System.out.printf("map(IDHM): %s%n", identityHashMapComma);
        System.out.printf("map(LHM): %s%n", linkedHashMapComma);
        System.out.printf("map(P): %s%n", properties);
        System.out.printf("map(SM): %s%n", sortedMap);
        System.out.printf("map(TM): %s%n", treeMap);
        System.out.printf("map(WHM): %s%n", weakHashMap);

        System.out.printf("set: %s%n", set);
        System.out.printf("set(LHS): %s%n", linkedHashSet);
        System.out.printf("set(HS): %s%n", hashSet);
        System.out.printf("set(TS): %s%n", treeSet);
        System.out.printf("set(SS): %s%n", sortedSet);

        System.out.printf("logger: %s%n", logger);
        System.out.printf("pattern: %s%n", pattern);
        System.out.printf("object.name: %s%n", objectName);
        System.out.printf("big.integer: %s%n", bigInteger);
        System.out.printf("big.decimal: %s%n", bigDecimal);
        System.out.printf("int: %s%n", intNumber);
        System.out.printf("url: %s%n", url);
        System.out.printf("uri: %s%n", uri);
        System.out.printf("file: %s%n", file);
        System.out.printf("stringBuilder: %s%n", stringBuilder);
        System.out.printf("stringBuffer:  %s%n", stringBuffer);
        return result;
    }
}
