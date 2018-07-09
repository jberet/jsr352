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

import javax.batch.api.chunk.ItemProcessor;
import javax.script.Invocable;
import javax.script.ScriptException;

import org.jberet.job.model.Properties;
import org.jberet.job.model.Script;
import org.jberet.runtime.context.StepContextImpl;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemProcessor} whose {@code processItem} method runs a script.
 */
final class ScriptItemProcessor extends ScriptArtifactBase implements ItemProcessor {
    public ScriptItemProcessor(final Script script, final Properties artifactProperties, final StepContextImpl stepContext) throws ScriptException {
        super(script, artifactProperties, stepContext);
    }

    @Override
    public Object processItem(final Object item) throws Exception {
        final Object result = compiledScript == null ? engine.eval(scriptContent) : compiledScript.eval();
        if (engine instanceof Invocable) {
            final Invocable invocable = (Invocable) engine;
            try {
                return invocable.invokeFunction(getFunctionName("processItem"), item);
            } catch (final NoSuchMethodException e) {
                //the script does not implement processItem method, so just return the original item as is
                return item;
            }
        } else {
            return result;
        }
    }
}
