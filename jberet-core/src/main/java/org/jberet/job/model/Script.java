/*
 * Copyright (c) 2013-2014 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.job.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.jberet._private.BatchLogger;
import org.jberet._private.BatchMessages;

/**
 * Represents a script element under an enclosing element represented by {@link org.jberet.job.model.RefArtifact}.
 * The script may be included inline in job XML as element text or CDATA, or reference an external resource via
 * {@code src} attribute. For example,
 * <p/>
 * <pre>
 *&lt;step id="batchletJavascriptSrc.step1"&gt;
 *  &lt;batchlet&gt;
 *      &lt;script src="javascript/simple-batchlet.js"/&gt;
 *  &lt;/batchlet&gt;
 *&lt;/step&gt;
 * </pre>
 *<pre>
 *&lt;step id="batchletJavascriptInline.step1"&gt;
 *&lt;batchlet&gt;
 *&lt;script type="javascript"&gt;
 *  function stop() {
 *      print('In stop function\n');
 *  }
 *
 *  //access built-in variables: jobContext, stepContext and batchProperties,
 *  //set job exit status to the value of testName property, and
 *  //return the value of testName property as step exit status,
 *
 *  function process() {
 *      print('jobName: ' + jobContext.getJobName() + '\n');
 *      print('stepName: ' + stepContext.getStepName() + '\n');
 *      var testName = batchProperties.get('testName');
 *      jobContext.setExitStatus(testName);
 *      return testName;
 *  }
 *    &lt;/script&gt;
 *  &lt;/batchlet&gt;
 *&lt;/step&gt;
 * </pre>
 */
public class Script implements Serializable, Cloneable {
    private static final long serialVersionUID = 3048730979495066553L;

    private static volatile ScriptEngineManager scriptEngineManager;

    String type;
    String src;
    String content;

    Script(final String type, final String src) {
        this.type = type;
        this.src = src;
    }

    /**
     * Gets the content of the script as string.
     * If the script is specified as inline in job XML, its content is returned.  Otherwise, this method tries to load
     * the content of the resource as specified in {@code scr attribute} fo the {@code script} element in job XML.
     *
     * @param classLoader class loader to load the resource
     * @return the string content of the script
     */
    public String getContent(final ClassLoader classLoader) {
        if (content != null) {
            return content;
        }
        InputStream is = classLoader.getResourceAsStream(src);
        if (is == null) {
            BatchLogger.LOGGER.tracef("script src %s is not loadable by ClassLoader, next try file", src);
            try {
                final File file = new File(src);
                if (file.exists() && file.isFile()) {
                    is = new FileInputStream(file);
                }
            } catch (final Exception e) {
                BatchLogger.LOGGER.tracef(e, "exception when loading script src %s as a file, next try URL", src);
            }
            if (is == null) {
                try {
                    final URL url = new URI(src).toURL();
                    is = url.openStream();
                } catch (final Exception e) {
                    BatchLogger.LOGGER.tracef(e, "exception when loading script src %s as URL", src);
                }
            }
        }
        if (is == null) {
            throw BatchMessages.MESSAGES.failToGetScriptContent(null, src);
        }

        java.util.Scanner scanner = null;
        try {
            scanner = new java.util.Scanner(is).useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        } finally {
            if (scanner != null) {
                try {
                    //closing the scannr also closes the InputStream source.
                    scanner.close();
                } catch (final Exception e) {
                    BatchLogger.LOGGER.tracef(e, "Failed to close resource %s", src);
                }
            }
        }
    }

    /**
     * Gets the type of the script, for example, {@code javascript}.
     * @return type of the script
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the javax.script.ScriptEngine for this script.
     *
     * @param classLoader class loader used to obtain the script engine
     * @return {@code ScriptEngine} for this script
     */
    public ScriptEngine getEngine(final ClassLoader classLoader) {
        ScriptEngineManager mgr = scriptEngineManager;
        if (mgr == null) {
            synchronized (Script.class) {
                mgr = scriptEngineManager;
                if (mgr == null) {
                    scriptEngineManager = mgr = new ScriptEngineManager(classLoader);
                }
            }
        }

        ScriptEngine engine = null;
        if (type != null) {
            engine = type.indexOf('/') > 0 ? mgr.getEngineByMimeType(type) : mgr.getEngineByName(type);
        } else if (src != null) {
            final int fileExtensionDot = src.lastIndexOf('.');
            if (fileExtensionDot < 0) {
                throw BatchMessages.MESSAGES.invalidScriptAttributes(type, src);
            }
            final String ext = src.substring(fileExtensionDot + 1);
            engine = mgr.getEngineByExtension(ext);
        }

        if (engine != null) {
            if (engine.getClass().getSimpleName().indexOf("Groovy") > -1) {
                engine.getContext().setAttribute("#jsr223.groovy.engine.keep.globals", "weak", ScriptContext.ENGINE_SCOPE);
            }
            return engine;
        }
        throw BatchMessages.MESSAGES.invalidScriptAttributes(type, src);
    }

    @Override
    protected Script clone() {
        final Script c = new Script(this.type, this.src);
        c.content = this.content;
        return c;
    }
}
