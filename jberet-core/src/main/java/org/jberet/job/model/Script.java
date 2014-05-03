/*
 * Copyright (c) 2013-2014 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.job.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.jberet._private.BatchLogger;
import org.jberet._private.BatchMessages;

/**
 * Represents a script element under an enclosing element represented by {@link org.jberet.job.model.RefArtifact}.
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
                    scanner.close();
                } catch (final Exception e) {
                    BatchLogger.LOGGER.tracef(e, "Failed to close resource %s", src);
                }
            }
        }
    }

    public String getType() {
        return type;
    }

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
                throw BatchMessages.MESSAGES.invalidScriptTypeOrFileExtension(type, src);
            }
            final String ext = src.substring(fileExtensionDot + 1);
            engine = mgr.getEngineByExtension(ext);
        }

        if (engine != null) {
            return engine;
        }
        throw BatchMessages.MESSAGES.invalidScriptTypeOrFileExtension(type, src);
    }

    @Override
    protected Script clone() {
        final Script c = new Script(this.type, this.src);
        c.content = this.content;
        return c;
    }
}
