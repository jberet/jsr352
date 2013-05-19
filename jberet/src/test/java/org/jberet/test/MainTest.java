/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
