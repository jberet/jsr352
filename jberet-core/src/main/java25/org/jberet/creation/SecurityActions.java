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

class SecurityActions {

    static void setFieldValue(final Field field, final Object obj, final Object value) throws Exception {            
        if(field.trySetAccessible()){
                field.set(obj,value);
            }
    }

    static Object getField(final Field field, final Object obj) throws Exception {
        if(field.trySetAccessible()){
            return field.get(obj);
        }
        return null;
        
    }

    static Object invokeMethod(final Method method, final Object obj) throws Exception {
        if(method.trySetAccessible()){
            return method.invoke(obj);
        }
        return null;
    }
}
