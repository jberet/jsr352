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

package org.jberet.test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.jberet.Main;
import org.junit.Test;

public class MainTest {
    public Map<? extends Number, ?> integers = new HashMap<Integer, String>();

    private static final String jobXmlName = "batchlet1.xml";

    @Test
    public void testMain() throws Exception {
        String args[] = {jobXmlName};
        Main.main(args);

    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Test
    public void foo() throws Exception {
//        Field f = MainTest.class.getField("integers");
//        Type ftype = f.getGenericType();
//        ParameterizedType pt = (ParameterizedType) ftype;
//
//        System.out.printf("f.getGenericType: %s%n", ftype);
//        System.out.printf("genericType class: %s%n", ftype.getClass());
//        System.out.printf("pt.getActualTypeArguments: %s%n", pt.getActualTypeArguments());
//        for (Type type : pt.getActualTypeArguments()) {
//            System.out.printf("actual type arg: %s, class: %s%n", type, type.getClass());
//        }
//
//        System.out.printf("# of type arguments: %s%n", pt.getActualTypeArguments().length);
//        System.out.printf("pt.getRawType: %s%n", pt.getRawType());
//        System.out.printf("pt.getOwnerType: %s%n", pt.getOwnerType());
//
//        final String[] names = {"Linux", "Mac OS X", "Windows", "Solaris", null};
//        System.out.printf("print names with String.valueOf(): %s%n", String.valueOf(names));

        Method toString = this.getClass().getDeclaredMethod("toString");
        System.out.printf("getDeclaredMethod(toString): %s%n", toString);
        System.out.printf("toString getDeclaringClass: %s%n", toString.getDeclaringClass());

        Class<?> superClass = this.getClass().getSuperclass();
        Method superString = superClass.getDeclaredMethod("toString");
        System.out.printf("Object toString: %s%n", superString);
        System.out.printf("super toString getDeclaringClass: %s%n", superString.getDeclaringClass());
    }
}
