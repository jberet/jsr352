package org.jberet.se._private;

import javax.batch.operations.BatchRuntimeException;

import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */

@MessageLogger(projectCode = "JBERET")
@ValidIdRange(min = 50000, max = 59999)
public interface SEBatchLogger {

    SEBatchLogger LOGGER = Logger.getMessageLogger(SEBatchLogger.class, "org.jberet.se");

    @Message(id = 50000, value = "Failed to load configuration file %s")
    BatchRuntimeException failToLoadConfig(@Cause Throwable th, String configFile);

    @Message(id = 50001, value = "The configuration file %s is not found in the classpath, and will use the default configuration.")
    @LogMessage(level = Logger.Level.TRACE)
    void useDefaultJBeretConfig(String configFile);

    @Message(id = 50002, value = "Failed to get a valid value for configuration property %s; current value is %s.")
    BatchRuntimeException failToGetConfigProperty(String propName, String value, @Cause Throwable throwable);
}
