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
import org.apache.commons.exec.ExecuteStreamHandler;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.jberet.support._private.SupportLogger;
import org.jberet.support._private.SupportMessages;

/**
 * This batchlet runs a native OS command in a sub-process asynchronously.
 * Main features supported include:
 * <ul>
 *     <li>specify command and arguments as a single line;
 *     <li>specify command and arguments as a comma-separated list of items, for easy handling of spaces in file paths;
 *     <li>custom working directory;
 *     <li>specify timeout as seconds so the OS commnad can timeout
 *     <li>the OS command process can be stopped;
 *     <li>passing custom environment variables;
 *     <li>map non-zero exit code from OS command process.
 * </ul>
 */
@Named
@Dependent
public class OsCommandBatchlet implements Batchlet {
    /**
     * Injected {@code StepContext}.
     */
    @Inject
    protected StepContext stepContext;

    /**
     * The OS command and its arguments as a single line.
     * <p>
     * For example,
     * <pre>
     * "diff out1.txt out2.txt"
     * "cp out1.txt /tmp"
     * </pre>
     *
     * Either this property or {@link #commandArray} property must be specified.
     * If both are present, this property takes precedence.
     * <p>
     * If a command argument contains whitespaces, the argument must be quoted
     * to prevent this argument being broken into multiple arguments.
     * Alternatively, {@link #commandArray} property can be used.
     *
     * @see #commandArray
     */
    @Inject
    @BatchProperty
    protected String commandLine;

    /**
     * The OS command and its arguments as a list of string values separated by comma (,).
     * <p>
     * For example,
     * <pre>
     * "diff, out1.txt, out2.txt"
     * "cp, out1.txt, /tmp"
     * </pre>
     *
     * Either this property or {@link #commandLine} property must be specified.
     * If both are present, {@link #commandLine} takes precedence.
     *
     * @see #commandLine
     */
    @Inject
    @BatchProperty
    protected List<String> commandArray;

    /**
     * The working directory for running the OS command. Optional property, and if not set,
     * it defaults to the current directory.
     */
    @Inject
    @BatchProperty
    protected File workingDir;

    /**
     * A comma-separated list of int numbers that signal the successful completion of the
     * OS command. Optional property, and if not set, it defaults to 0. If the OS command
     * process is known to return non-zero exit code upon successful execution,
     * this property must be set as such to mark the command execution as successful.
     * <p>
     * For example,
     * <pre>
     * "90, 91, 92"
     * </pre>
     */
    @Inject
    @BatchProperty
    protected int[] commandOkExitValues;

    /**
     * The timeout as number of seconds. After the {@code timeoutSeconds} elapses, and the
     * OS command still has not finished, its process will be destroyed, and the batch
     * job execution will be marked as failed.
     * <p>
     * Optional property, and if not set, it defaults to no timeout.
     */
    @Inject
    @BatchProperty
    protected long timeoutSeconds;

    /**
     * Custom environment variables to be used when running the OS command.
     * Optional property, and if not set, it defaults to using the environment
     * of parent process. If set, it will represent the entire environment in
     * the new process, and the parent environment will not be inherited.
     * <p>
     * For example,
     * <pre>
     * "LANG=UTF-8, TMPDIR=/tmp"
     * </pre>
     */
    @Inject
    @BatchProperty
    protected Map<String, String> environment;

    /**
     * Fully-qualified name of a class that implements {@code org.apache.commons.exec.ExecuteStreamHandler},
     * which handles the input and output of the subprocess.
     *
     * @see "org.apache.commons.exec.ExecuteStreamHandler"
     */
    @Inject
    @BatchProperty
    protected Class streamHandler;

    private ExecuteWatchdog watchdog;

    private volatile boolean isStopped;

    /**
     * {@inheritDoc}
     * <p>
     * This method runs the OS command.
     * If the command completes successfully, its process exit code is returned.
     * If there is exception while running the OS command, which may be
     * caused by timeout, the command being stopped, or other errors, the process
     * exit code is set as the step exit status, and the exception is thrown.
     *
     * @return the OS command process exit code
     *
     * @throws Exception upon errors
     */
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
        if (streamHandler != null) {
            executor.setStreamHandler((ExecuteStreamHandler) streamHandler.newInstance());
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

    /**
     * {@inheritDoc}
     * <p>
     * This method tries to destroy the process running the OS command.
     *
     * @throws Exception upon errors
     */
    @Override
    public void stop() throws Exception {
        if (watchdog != null) {
            isStopped = true;
            watchdog.destroyProcess();
        }
    }
}
