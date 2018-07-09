/*
 * Copyright (c) 2012-2018 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.se._private;

import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBERET")
//@ValidIdRange(min = 50000, max = 59999)
@ValidIdRange(min = 50500, max = 50999)
public interface SEBatchLogger {
    SEBatchLogger LOGGER = Logger.getMessageLogger(SEBatchLogger.class, "org.jberet.se");

    @Message(id = 50500, value = "The configuration file %s is not found in the classpath, and will use the default configuration.")
    @LogMessage(level = Logger.Level.TRACE)
    void useDefaultJBeretConfig(String configFile);

    @Message(id = 50501,
    value = "Usage:%njava -classpath ... [other java options] org.jberet.se.Main jobXMLName [jobParameter1=value1 jobParameter2=value2 ...]%nThe following application args are invalid: %s")
    @LogMessage(level = Logger.Level.ERROR)
    void usage(String[] args);

}
