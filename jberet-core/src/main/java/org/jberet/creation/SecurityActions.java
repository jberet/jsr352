/*
 * Copyright (c) 2014-2026 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.creation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

class SecurityActions {

    static void setFieldValue(final Field field, final Object obj, final Object value) throws Exception {
        if(System.getSecurityManager() != null){
            AccessController.doPrivileged(
                (PrivilegedExceptionAction <Void>) () -> {
                    if(field.trySetAccessible()){
                        field.set(obj,value);
                    } else {
                        throw new java.lang.reflect.InaccessibleObjectException("Unable to make field accessible: " + field);
                    }
                    return null;
                });
        }
        else {
            if(field.trySetAccessible()){
                field.set(obj,value);
            } else {
                throw new java.lang.reflect.InaccessibleObjectException("Unable to make field accessible: " + field);
            }
        }
    }

    static Object getField(final Field field, final Object obj) throws Exception {
        if(System.getSecurityManager() != null){
            return AccessController.doPrivileged(
                (PrivilegedExceptionAction<Object>) () -> {
                    if(field.trySetAccessible()){
                        return field.get(obj);
                    } else {
                        throw new java.lang.reflect.InaccessibleObjectException("Unable to make field accessible: " + field);
                    }
                }
            );
        }
        else {
            if(field.trySetAccessible()){
                return field.get(obj);
            } else {
                throw new java.lang.reflect.InaccessibleObjectException("Unable to make field accessible: " + field);
            }
        }
    }

    static Object invokeMethod(final Method method, final Object obj) throws Exception {
        if(System.getSecurityManager() != null){
            return AccessController.doPrivileged(
                (PrivilegedExceptionAction<Object>) () -> {
                    if(method.trySetAccessible()){
                        return method.invoke(obj);
                    } else {
                        throw new java.lang.reflect.InaccessibleObjectException("Unable to make method accessible: " + method);
                    }
                }
            );
        }
        else {
            if(method.trySetAccessible()){
                return method.invoke(obj);
            } else {
                throw new java.lang.reflect.InaccessibleObjectException("Unable to make method accessible: " + method);
            }
        }
    }
}
