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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.batch.api.BatchProperty;
import javax.batch.api.Batchlet;
import javax.batch.operations.BatchRuntimeException;
import javax.batch.runtime.context.StepContext;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import org.jberet.support._private.SupportLogger;
import org.jberet.support._private.SupportMessages;

/**
 * This batchlet runs a native OS command in a sub-process asynchronously.
 * Main features supported include:
 * <ul>
 * <li>specify command and arguments as a single line;
 * <li>specify command and arguments as a comma-separated list of items, for easy handling of spaces in file paths;
 * <li>custom working directory;
 * <li>specify timeout as seconds so the OS commnad can timeout
 * <li>the OS command process can be stopped;
 * <li>passing custom environment variables;
 * <li>map non-zero exit code from OS command process.
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
     * Fully-qualified name of a class that implements {@link StreamHandler},
     * which handles the input and output of the subprocess.
     *
     * @see StreamHandler
     */
    @Inject
    @BatchProperty
    protected Class streamHandler;

    private final Object lock = new Object();

    // Guarded by lock
    private StreamHandler handler;
    // Guarded by lock
    private Process process;
    // Guarded by lock
    private boolean isStopped;

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
        final List<String> cmd = new ArrayList<>();
        if (commandLine != null) {
            cmd.addAll(parse(commandLine));
        } else {
            if (commandArray == null) {
                throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, null, "commandArray");
            } else if (commandArray.isEmpty()) {
                throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, commandArray.toString(), "commandArray");
            }
            cmd.addAll(commandArray);
        }

        // Create the process builder based on the commands
        final ProcessBuilder processBuilder = new ProcessBuilder(cmd);

        // Set the working directory for the process
        if (workingDir != null) {
            processBuilder.directory(workingDir);
        }
        // Set the environment variables for the process
        if (environment != null) {
            processBuilder.environment().putAll(environment);
        }

        // Validate the working directory
        File workingDirectory = processBuilder.directory();
        if (workingDirectory == null) {
            workingDirectory = new File(".");
        }
        if (!workingDirectory.exists()) {
            throw SupportMessages.MESSAGES.invalidDirectory(workingDirectory.getAbsolutePath());
        }
        SupportLogger.LOGGER.runCommand(cmd, workingDirectory.getAbsolutePath());

        // Start the process and the stream handler if defined
        final Process process = processBuilder.start();
        StreamHandler handler = null;
        if (streamHandler != null) {
            handler = (StreamHandler) streamHandler.newInstance();
            handler.setProcessErrorStream(process.getErrorStream());
            handler.setProcessInputStream(process.getOutputStream());
            handler.setProcessOutputStream(process.getInputStream());
            handler.start();
        }
        synchronized (lock) {
            this.process = process;
            this.handler = handler;
        }

        try {

            if (timeoutSeconds > 0) {
                // If defined wait for the number of seconds, if the process has not terminated the process should be
                // destroyed
                if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            }
            // Wait for the process to exit
            final int exitCode = process.waitFor();

            // If the exit code was not expected or not a normal exit code (0 is considered normal) fail.
            if (isFailure(exitCode) || exitCode != 0) {
                stepContext.setExitStatus(String.valueOf(exitCode));
                final boolean started;
                synchronized (lock) {
                    started = !isStopped;
                }
                // Throw an exception if started otherwise log the error
                if (started) {
                    throw new BatchRuntimeException(SupportMessages.MESSAGES.processExecutionFailure(exitCode, cmd));
                } else {
                    SupportLogger.LOGGER.warn(SupportMessages.MESSAGES.processExecutionFailure(exitCode, cmd));
                }
            }
            return String.valueOf(exitCode);
        } finally {
            if (handler != null) {
                handler.stop();
            }
        }
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
        synchronized (lock) {
            try {
                if (process != null) {
                    isStopped = true;
                    // Destroy the process. The waitFor() in the process() should handle the rest of the processing.
                    process.destroyForcibly();
                }
            } finally {
                process = null;
                if (handler != null) {
                    handler.stop();
                }
            }
        }
    }

    private boolean isFailure(final int exitCode) {
        if (commandOkExitValues == null) {
            return false;
        }
        for (int okValue : commandOkExitValues) {
            if (okValue == exitCode) {
                return false;
            }
        }
        return true;
    }

    /**
     * Parses a command into a collection of strings for the {@link ProcessBuilder}.
     *
     * @param cmd the command to parse
     *
     * @return the parsed commands or an empty list
     */
    private static Collection<String> parse(final String cmd) {
        if (cmd == null) return Collections.emptyList();
        final Collection<String> arguments = new ArrayList<>();
        boolean inDoubleQuote = false;
        boolean inSingleQuote = false;
        StringBuilder sb = new StringBuilder();
        for (char c : cmd.toCharArray()) {
            if (c == '"') {
                inDoubleQuote = !inSingleQuote && !inDoubleQuote;
            } else if (c == '\'') {
                inSingleQuote = !inSingleQuote && !inDoubleQuote;
            } else if (c == ' ' && !inDoubleQuote && !inSingleQuote) {
                arguments.add(sb.toString());
                sb.setLength(0);
                continue;
            }
            sb.append(c);
        }
        if (inDoubleQuote || inSingleQuote) {
            throw new IllegalArgumentException("Missing trailing quote in: " + cmd);
        }
        if (sb.length() > 0) {
            arguments.add(sb.toString());
        }
        return arguments;
    }
}
