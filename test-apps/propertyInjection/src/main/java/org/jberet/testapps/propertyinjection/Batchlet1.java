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
    Class<?> cls;

    @Inject @BatchProperty(name = "color")
    Color color;

    @Inject @BatchProperty(name = "inet.address")
    InetAddress inetAddress;

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

    @Inject @BatchProperty(name = "map")
    HashMap<String, String> hashMap;

    @Inject @BatchProperty(name = "map")
    HashMap<?, ?> hashMapWild;

    @Inject @BatchProperty(name = "map")
    Hashtable<String, ?> hashtableWildValue;

    @Inject @BatchProperty(name = "map")
    Hashtable<String, String> hashtable;

    @Inject @BatchProperty(name = "map")
    IdentityHashMap<String, String> identityHashMap;

    @Inject @BatchProperty(name = "map")
    LinkedHashMap<String, String> linkedHashMap;

    @Inject @BatchProperty(name = "map")
    Properties properties;

    @Inject @BatchProperty(name = "map")
    SortedMap<String, String> sortedMap;

    @Inject @BatchProperty(name = "map")
    TreeMap<String, String> treeMap;

    @Inject @BatchProperty(name = "map")
    WeakHashMap<String, String> weakHashMap;

    @Inject @BatchProperty(name = "map.date")
    HashMap<String, Date> hashMapDate;

    @Inject @BatchProperty(name = "map.date")
    Hashtable<String, ?> hashtableStringWild;


    @Inject @BatchProperty(name = "set")
    Set<String> set;

    @Inject @BatchProperty(name = "set")
    Set<?> setWild;

    @Inject @BatchProperty(name = "set")
    Set rawSet;

    @Inject @BatchProperty(name = "set")
    Set<Float> setFloat;

    @Inject @BatchProperty(name = "set")
    LinkedHashSet<String> linkedHashSet;

    @Inject @BatchProperty(name = "set")
    LinkedHashSet<Float> linkedHashSetFloat;

    @Inject @BatchProperty(name = "set")
    HashSet<String> hashSet;

    @Inject @BatchProperty(name = "set")
    HashSet<?> hashSetWild;

    @Inject @BatchProperty(name = "set")
    HashSet<Float> hashSetFloat;

    @Inject @BatchProperty(name = "set")
    TreeSet<String> treeSet;

    @Inject @BatchProperty(name = "set")
    TreeSet<Float> treeSetFloat;

    @Inject @BatchProperty(name = "set")
    SortedSet<String> sortedSet;

    @Inject @BatchProperty(name = "set")
    SortedSet<Float> sortedSetFloat;


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

    @Inject @BatchProperty(name = "list")
    private ArrayList<String> arrayList;

    @Inject @BatchProperty(name = "list")
    ArrayList<?> arrayListWild;

    @Inject @BatchProperty(name = "list")
    private LinkedList<String> linkedList;

    @Inject @BatchProperty(name = "list")
    LinkedList<?> linkedListWild;

    @Inject @BatchProperty(name = "list")
    private Vector<String> vectorList;

    @Inject @BatchProperty(name = "list")
    private List<Double> listDouble;

    @Inject @BatchProperty(name = "list")
    private ArrayList<Double> arrayListDouble;

    @Inject @BatchProperty(name = "list")
    private LinkedList<Double> linkedListDouble;

    @Inject @BatchProperty(name = "list")
    private Vector<Double> vectorDouble;

    @Inject @BatchProperty(name = "list.date")
    List<Date> listDate;

    @Inject @BatchProperty(name = "list.date")
    List listDateString;

    @Inject @BatchProperty(name = "list.enum")
    List<Color> listColor;

    @Inject @BatchProperty(name = "list")
    private String[] listStringArray;

    @Inject @BatchProperty(name = "list")
    double[] listdoubleArray;

    @Inject @BatchProperty(name = "list")
    Double[] listDoubleArray;

    @Inject @BatchProperty(name = "list.date")
    Date[] listDateArray;

    @Inject @BatchProperty(name = "list.enum")
    Color[] listEnumArray;

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

        System.out.printf("list:         %s%n", list);
        System.out.printf("list(raw):    %s%n", rawList);
        System.out.printf("list<?>:      %s%n", listWild);
        System.out.printf("list(AL):     %s%n", arrayList);
        System.out.printf("list(AL,?):   %s%n", arrayListWild);
        System.out.printf("list(LL):     %s%n", linkedList);
        System.out.printf("list(LL,?):   %s%n", linkedListWild);
        System.out.printf("list(V):      %s%n", vectorList);
        System.out.printf("List<Double>: %s%n", listDouble);
        for (Double d : listDouble) {
            System.out.printf("    * 2 = %s%n", d * 2);
        }
        System.out.printf("list(AL,D):   %s%n", arrayListDouble);
        System.out.printf("list(LL,D):   %s%n", linkedListDouble);
        System.out.printf("list(V,D):    %s%n", vectorDouble);
        System.out.printf("list.date<D>: %s%n", listDate);
        System.out.printf("list.date(S): %s%n", listDateString);
        System.out.printf("list.enum(C): %s%n", listColor);

        System.out.printf("list String[] len: %s%n", listStringArray.length);
        for (int i = 0; i < listStringArray.length; i++) {
            System.out.printf("    listStringArray[%s] = %s%n", i, listStringArray[i]);
        }
        System.out.printf("list double[] len: %s%n", listdoubleArray.length);;
        for (int i = 0; i < listdoubleArray.length; i++) {
            System.out.printf("    listdoubleArray[%s] = %s%n", i, listdoubleArray[i]);
        }
        System.out.printf("list Double[] len: %s%n", listDoubleArray.length);;
        for (int i = 0; i < listDoubleArray.length; i++) {
            System.out.printf("    listDoubleArray[%s] = %s%n", i, listDoubleArray[i]);
        }
        System.out.printf("list Date[] len: %s%n", listDateArray.length);;
        for (int i = 0; i < listDateArray.length; i++) {
            System.out.printf("    listDateArray[%s] = %s%n", i, listDateArray[i]);
        }
        System.out.printf("list enum Color[] len: %s%n", listEnumArray.length);;
        for (int i = 0; i < listEnumArray.length; i++) {
            System.out.printf("    listEnumArray[%s] = %s%n", i, listEnumArray[i]);
        }

        System.out.printf("map:              %s%n", map);
        System.out.printf("map(raw):         %s%n", mapRaw);
        System.out.printf("map<?,?>:         %s%n", mapWild);
        System.out.printf("map<s,?>:         %s%n", mapWildValue);
        System.out.printf("map(HM):          %s%n", hashMap);
        System.out.printf("map(HM,?,?):      %s%n", hashMapWild);
        System.out.printf("map(HT):          %s%n", hashtable);
        System.out.printf("map(HT,S,?):      %s%n", hashtableWildValue);
        System.out.printf("map(IDHM):        %s%n", identityHashMap);
        System.out.printf("map(LHM):         %s%n", linkedHashMap);
        System.out.printf("map(P):           %s%n", properties);
        System.out.printf("map(SM):          %s%n", sortedMap);
        System.out.printf("map(TM):          %s%n", treeMap);
        System.out.printf("map(WHM):         %s%n", weakHashMap);
        System.out.printf("map.date(HM,D):   %s%n", hashMapDate);
        System.out.printf("map.date(HT,S,?): %s%n", hashtableStringWild);

        System.out.printf("set:        %s%n", set);
        System.out.printf("set(raw):   %s%n", rawSet);
        System.out.printf("set<?>:     %s%n", setWild);
        System.out.printf("set(LHS):   %s%n", linkedHashSet);
        System.out.printf("set(HS):    %s%n", hashSet);
        System.out.printf("set(HS,?):  %s%n", hashSetWild);
        System.out.printf("set(TS):    %s%n", treeSet);
        System.out.printf("set(SS):    %s%n", sortedSet);
        System.out.printf("Set<Float>: %s%n", setFloat);
        for (Float f : setFloat) {
            System.out.printf("    * 2 = %s%n", f * 2);
        }
        System.out.printf("set(LHS,F): %s%n", linkedHashSetFloat);
        System.out.printf("set(HS,F):  %s%n", hashSetFloat);
        System.out.printf("set(TS,F):  %s%n", treeSetFloat);
        System.out.printf("set(SS,F):  %s%n", sortedSetFloat);

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
