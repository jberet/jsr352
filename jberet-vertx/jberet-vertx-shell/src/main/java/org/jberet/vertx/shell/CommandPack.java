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

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.shell.command.Command;
import io.vertx.ext.shell.command.CommandResolver;
import io.vertx.ext.shell.spi.CommandResolverFactory;

@SuppressWarnings("unused")
public final class CommandPack implements CommandResolverFactory {
    @Override
    public void resolver(final Vertx vertx, final Handler<AsyncResult<CommandResolver>> resolveHandler) {
        List<Command> commands = new ArrayList<>();
        commands.add(Command.create(vertx, StartJobCommand.class));
        commands.add(Command.create(vertx, ListJobsCommand.class));
        commands.add(Command.create(vertx, AbandonJobExecutionCommand.class));
        commands.add(Command.create(vertx, CountJobInstancesCommand.class));
        commands.add(Command.create(vertx, GetJobExecutionCommand.class));
        commands.add(Command.create(vertx, GetStepExecutionCommand.class));
        commands.add(Command.create(vertx, ListJobExecutionsCommand.class));
        commands.add(Command.create(vertx, ListJobInstancesCommand.class));
        commands.add(Command.create(vertx, ListStepExecutionsCommand.class));
        commands.add(Command.create(vertx, RestartJobExecutionCommand.class));
        commands.add(Command.create(vertx, StopJobExecutionCommand.class));

        // Add another command
//        commands.add(CommandBuilder.command("another-command").processHandler(process -> {
        // Handle process
//        }).build(vertx));

        resolveHandler.handle(Future.succeededFuture(() -> commands));
    }
}
