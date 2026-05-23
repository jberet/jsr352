/*
 * Copyright (c) 2014-2016 Red Hat, Inc. and/or its affiliates.
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

/*
 * This class is used to perform privileged actions when security manager is enabled.
 * It uses System.getSecurityManager to check if security manager is enabled and then 
 * performs the action accordingly.
 * 
 * It is used for JobXmlParser to:
 * - Set the fields and invoke methods of the job xml parser classes.
 * - Get the fields of the job xml parser classes.
*/

class SecurityActions {

    static void setFieldValue(final Field field, final Object obj, final Object value) throws Exception {
        if(System.getSecurityManager() != null){
            AccessController.doPrivileged(
                (PrivilegedExceptionAction <Void>) () -> {
                    if(field.trySetAccessible()){
                        field.set(obj,value);
                    }
                    return null; 
                });
        }
        else {
            if(field.trySetAccessible()){
                field.set(obj,value);
            }
        }
    }

    static Object getField(final Field field, final Object obj) throws Exception {
        if(System.getSecurityManager() != null){
            return AccessController.doPrivileged(
                (PrivilegedExceptionAction<Object>) () -> {
                if(field.trySetAccessible()){
                    return field.get(obj);
                }
                return null;
            }
            );
        }
        else {
            if(field.trySetAccessible()){
                return field.get(obj);
            }
            return null;
        }
    }

    static Object invokeMethod(final Method method, final Object obj) throws Exception {
        if(System.getSecurityManager() != null){
            return AccessController.doPrivileged(
                (PrivilegedExceptionAction<Object>) () -> {
                    if(method.trySetAccessible()){
                        return method.invoke(obj);
                    }
                    return null;
                }
            );
        }
        else {
            if(method.trySetAccessible()){
                return method.invoke(obj);
            }
            return null;
        }
    }
}
