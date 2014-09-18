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

import javax.batch.api.Batchlet;
import javax.script.Invocable;
import javax.script.ScriptException;

import org.jberet.job.model.Properties;
import org.jberet.job.model.Script;
import org.jberet.runtime.context.StepContextImpl;

/**
 * An implementation of {@code javax.batch.api.Batchlet} whose {@code process} method runs a script.
 */
final class ScriptBatchlet extends ScriptArtifactBase implements Batchlet {

    public ScriptBatchlet(final Script script, final Properties artifactProperties, final StepContextImpl stepContext) throws ScriptException {
        super(script, artifactProperties, stepContext);
    }

    @Override
    public String process() throws Exception {
        Object result = compiledScript == null ? engine.eval(scriptContent) : compiledScript.eval();
        if (engine instanceof Invocable) {
            final Invocable invocable = (Invocable) engine;

            try {
                result = invocable.invokeFunction(getFunctionName("process"));
            } catch (final NoSuchMethodException e) {
                //ignore
            }
        }

        return result == null ? null : result.toString();
    }

    @Override
    public void stop() throws Exception {
        if (engine instanceof Invocable) {
            try {
                ((Invocable) engine).invokeFunction(getFunctionName("stop"));
            } catch (final NoSuchMethodException e) {
                //ignore
            }
        }
    }
}
