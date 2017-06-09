/*
 * Copyright (c) 2017 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.vertx.shell;

import java.util.Set;

import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.ext.shell.command.CommandProcess;

@SuppressWarnings("unused")
@Name("list-jobs")
@Summary("List batch jobs")
public final class ListJobsCommand extends CommandBase {
    /**
     * {@inheritDoc}
     */
    @Override
    public void process(final CommandProcess process) {
        try {
            final Set<String> jobNames = jobOperator.getJobNames();
            process.write(String.format("Batch jobs: %s%n", jobNames));
            process.end();
        } catch (Exception e) {
            failed(process, e);
        }
    }
}
