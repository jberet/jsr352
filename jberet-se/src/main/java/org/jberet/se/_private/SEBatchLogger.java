/*
 * Copyright (c) 2012-2018 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
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

}
