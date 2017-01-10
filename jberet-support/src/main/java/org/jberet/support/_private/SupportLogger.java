/*
 * Copyright (c) 2014-2017 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.support._private;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

@MessageLogger(projectCode = "JBERET")
@ValidIdRange(min = 60500, max = 60999)
public interface SupportLogger extends BasicLogger {
    SupportLogger LOGGER = Logger.getMessageLogger(SupportLogger.class, "org.jberet.support");

    @Message(id = 60500, value = "The CellProcessor value may be missing an ending single quote: %s")
    @LogMessage(level = Logger.Level.WARN)
    void maybeMissingEndQuote(String line);

    @Message(id = 60501, value = "Opening resource %s in %s")
    @LogMessage(level = Logger.Level.INFO)
    void openingResource(String resource, Class<?> cls);

    @Message(id = 60502, value = "Closing resource %s in %s")
    @LogMessage(level = Logger.Level.INFO)
    void closingResource(String resource, Class<?> cls);

    @Message(id = 60503, value = "Failed to write Excel workbook %s to output resource %s")
    @LogMessage(level = Logger.Level.WARN)
    void failToWriteWorkbook(@Cause Throwable th, String workbook, String resource);

    @Message(id = 60504, value = "About to run command %s, with arguments %s, in working directory %s")
    @LogMessage(level = Logger.Level.INFO)
    void runCommand(String command, String args, String workingDir);

}