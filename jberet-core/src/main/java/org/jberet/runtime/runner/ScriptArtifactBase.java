/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.runtime.runner;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.jberet._private.BatchLogger;
import org.jberet.job.model.Properties;
import org.jberet.job.model.Script;
import org.jberet.runtime.context.StepContextImpl;

/**
 * Base class for batch artifacts that run a script.
 */
abstract class ScriptArtifactBase {
    final Script script;
    final Properties artifactProperties;
    final StepContextImpl stepContext;

    final ScriptEngine engine;
    CompiledScript compiledScript;
    final String scriptContent;

    Map<String, String> methodMapping;

    /**
     * Names of the optional functions found not to be implemented by the script, so they are not invoked again.
     */
    private final Set<String> missingFunctions = ConcurrentHashMap.newKeySet();

    public ScriptArtifactBase(final Script script, final Properties artifactProperties, final StepContextImpl stepContext) throws ScriptException {
        this.script = script;
        this.artifactProperties = artifactProperties;
        this.stepContext = stepContext;

        final ClassLoader classLoader = stepContext.getClassLoader();
        scriptContent = script.getContent(classLoader);
        this.engine = script.getEngine(classLoader);
        setEngineScopeAttributes();
        if (engine instanceof Compilable) {
            compiledScript = ((Compilable) engine).compile(scriptContent);
        }

        if (artifactProperties != null) {
            final String methodMappingVal = artifactProperties.get("methodMapping");
            if (methodMappingVal != null) {
                methodMapping = new HashMap<String, String>();
                final String[] pairs = methodMappingVal.split(",");
                for (final String pair : pairs) {
                    final String[] keyValue = pair.split("=");
                    methodMapping.put(keyValue[0].trim(), keyValue[1].trim());
                }
            }
        }
    }

    void setEngineScopeAttributes() {
        final ScriptContext scriptContext = this.engine.getContext();
        try {
            scriptContext.setAttribute("jobContext", stepContext.getJobContext(), ScriptContext.ENGINE_SCOPE);
            scriptContext.setAttribute("stepContext", stepContext, ScriptContext.ENGINE_SCOPE);
            scriptContext.setAttribute("batchProperties", Properties.toJavaUtilProperties(artifactProperties), ScriptContext.ENGINE_SCOPE);
        } catch (final Exception e) {
            BatchLogger.LOGGER.failToSetAttributesToScriptContext(e, engine.toString(), script.getType());
        }
    }

    String getFunctionName(final String batchApiMethodName) {
        if (methodMapping == null) {
            return batchApiMethodName;
        }
        final String functionName = methodMapping.get(batchApiMethodName);
        return functionName == null ? batchApiMethodName : functionName;
    }

    /**
     * Invokes the script function mapped to {@code batchApiMethodName}, which the script is not required to implement.
     * A {@code NoSuchMethodException} is treated as "function not implemented" only when its message refers to the
     * invoked function, or has no message; otherwise it was raised while running the function (e.g., some engines
     * report a call to an undefined method inside the function body this way), and is rethrown.
     * A function found not to be implemented is not invoked again.
     *
     * @param invocable the script engine as {@code Invocable}
     * @param defaultValue the value to return if the script does not implement the function
     * @param batchApiMethodName the batch API method name, which may be mapped to a different function name
     * @param args the arguments to pass to the function
     * @return the value returned by the function, or {@code defaultValue} if the script does not implement it
     * @throws Exception if the function fails
     */
    Object invokeOptionalFunction(final Invocable invocable, final Object defaultValue,
                                  final String batchApiMethodName, final Object... args) throws Exception {
        final String functionName = getFunctionName(batchApiMethodName);
        if (missingFunctions.contains(functionName)) {
            return defaultValue;
        }
        try {
            return invocable.invokeFunction(functionName, args);
        } catch (final NoSuchMethodException e) {
            if (!isFunctionNotImplemented(e, functionName)) {
                throw e;
            }
            missingFunctions.add(functionName);
            BatchLogger.LOGGER.debugf("The script does not implement function %s, skipping it: %s", functionName, e.getMessage());
            return defaultValue;
        }
    }

    private static boolean isFunctionNotImplemented(final NoSuchMethodException e, final String functionName) {
        final String message = e.getMessage();
        return message == null ||
                Pattern.compile("(?<![\\w$])" + Pattern.quote(functionName) + "(?![\\w$])").matcher(message).find();
    }
}
