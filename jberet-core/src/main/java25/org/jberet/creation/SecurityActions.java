package org.jberet.creation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

import org.wildfly.security.manager.WildFlySecurityManager;

/*
 * This class is used to perform privileged actions when security manager is enabled.
 * It uses WildFlySecurityManager to check if security manager is enabled and then 
 * performs the action accordingly.
 * 
 * It is used for JobXmlParser to:
 * - Set the fields and invoke methods of the job xml parser classes.
 * - Get the fields of the job xml parser classes.
*/

class SecurityActions {

    static void setFieldValue(final Field field, final Object obj, final Object value) throws Exception {
        if(WildFlySecurityManager.isChecking()){
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
        if(WildFlySecurityManager.isChecking()){
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
        if(WildFlySecurityManager.isChecking()){
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
