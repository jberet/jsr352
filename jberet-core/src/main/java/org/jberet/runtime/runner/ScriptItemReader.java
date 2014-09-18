/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.runtime.runner;

import java.io.Serializable;
import javax.batch.api.chunk.ItemReader;
import javax.script.Invocable;
import javax.script.ScriptException;

import org.jberet._private.BatchMessages;
import org.jberet.job.model.Properties;
import org.jberet.job.model.Script;
import org.jberet.runtime.context.StepContextImpl;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemReader} whose methods run corresponding functions in a script.
 */
final class ScriptItemReader extends ScriptArtifactBase implements ItemReader {
    private final Invocable invocable;

    public ScriptItemReader(final Script script, final Properties artifactProperties, final StepContextImpl stepContext) throws ScriptException {
        super(script, artifactProperties, stepContext);
        if (engine instanceof Invocable) {
            invocable = (Invocable) engine;
        } else {
            throw BatchMessages.MESSAGES.scriptNotInvocable(scriptContent);
        }
        if (compiledScript != null) {
            compiledScript.eval();
        } else {
            engine.eval(scriptContent);
        }
    }

    @Override
    public void open(final Serializable checkpoint) throws Exception {
        try {
            invocable.invokeFunction(getFunctionName("open"), checkpoint);
        } catch (final NoSuchMethodException e) {
            //the script does not implement open method, so just skip it
        }
    }

    @Override
    public void close() throws Exception {
        try {
            invocable.invokeFunction(getFunctionName("close"));
        } catch (final NoSuchMethodException e) {
            //the script does not implement close method, so just skip it
        }
    }

    @Override
    public Object readItem() throws Exception {
        return invocable.invokeFunction(getFunctionName("readItem"));
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        try {
            return (Serializable) invocable.invokeFunction(getFunctionName("checkpointInfo"));
        } catch (final NoSuchMethodException e) {
            //the script does not implement checkpointInfo method, so just return null
            return null;
        }
    }
}
