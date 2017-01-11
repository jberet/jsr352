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

package org.jberet.support.io;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.batch.api.BatchProperty;
import javax.batch.api.Batchlet;
import javax.batch.runtime.context.StepContext;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.jberet.support._private.SupportLogger;
import org.jberet.support._private.SupportMessages;

@Named
@Dependent
public class OsCommandBatchlet implements Batchlet {
    @Inject
    protected StepContext stepContext;

    @Inject
    @BatchProperty
    protected String commandLine;

    @Inject
    @BatchProperty
    protected List<String> commandArray;

    @Inject
    @BatchProperty
    protected File workingDir;

    @Inject
    @BatchProperty
    protected int[] commandOkExitValues;

    @Inject
    @BatchProperty
    protected long timeoutSeconds;

    @Inject
    @BatchProperty
    protected Map<String, String> environment;

    private ExecuteWatchdog watchdog;

    private volatile boolean isStopped;

    @Override
    public String process() throws Exception {


        final DefaultExecutor executor = new DefaultExecutor();
        final CommandLine commandLineObj;
        if (commandLine != null) {
            commandLineObj = CommandLine.parse(commandLine);
        } else {
            if (commandArray == null) {
                throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, null, "commandArray");
            } else if (commandArray.isEmpty()) {
                throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, commandArray.toString(), "commandArray");
            }
            commandLineObj = new CommandLine(commandArray.get(0));
            final int len = commandArray.size();
            if (len > 1) {
                for (int i = 1; i < len; i++) {
                    commandLineObj.addArgument(commandArray.get(i));
                }
            }
        }

        if (workingDir != null) {
            executor.setWorkingDirectory(workingDir);
        }

        SupportLogger.LOGGER.runCommand(commandLineObj.getExecutable(),
                Arrays.toString(commandLineObj.getArguments()), executor.getWorkingDirectory().getAbsolutePath());

        if (commandOkExitValues != null) {
            executor.setExitValues(commandOkExitValues);
        }

        watchdog = new ExecuteWatchdog(timeoutSeconds > 0 ? timeoutSeconds * 1000 : ExecuteWatchdog.INFINITE_TIMEOUT);
        executor.setWatchdog(watchdog);

        executor.setProcessDestroyer(new ShutdownHookProcessDestroyer());
        final DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
        executor.execute(commandLineObj, environment, resultHandler);
        resultHandler.waitFor();

        final ExecuteException exception = resultHandler.getException();
        if (exception != null) {
            stepContext.setExitStatus(String.valueOf(resultHandler.getExitValue()));
            if (!isStopped) {
                throw exception;
            } else {
                SupportLogger.LOGGER.warn("", exception);
            }
        }
        return String.valueOf(resultHandler.getExitValue());
    }

    @Override
    public void stop() throws Exception {
        if (watchdog != null) {
            isStopped = true;
            watchdog.destroyProcess();
        }
    }
}
