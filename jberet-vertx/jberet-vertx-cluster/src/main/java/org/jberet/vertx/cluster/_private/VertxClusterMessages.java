package org.jberet.vertx.cluster._private;

import java.util.concurrent.TimeUnit;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.logging.annotations.ValidIdRange;

@MessageBundle(projectCode = "JBERET")
@ValidIdRange(min = 74000, max = 74499)
public interface VertxClusterMessages {
    VertxClusterMessages MESSAGES = Messages.getBundle(VertxClusterMessages.class);

    @Message(id = 74000, value = "Failed to set up Vertx cluster within %s %s")
    IllegalStateException failToInitVertxClusterTimeout(long timeout, TimeUnit timeUnit);

    @Message(id = 74001, value = "Failed to set up Vertx cluster")
    IllegalStateException failToInitVertxCluster(@Cause Throwable cause);

    @Message(id = 74002, value = "The Vertx is not clustered: %s")
    IllegalStateException vertxNotClustered(Object vertx);

    @Message(id = 74003, value = "Failed to receive partition info at remote node")
    IllegalStateException failedToReceivePartitionInfo(@Cause Throwable cause);

    @Message(id = 74004, value = "Failed to receive partition collector data from remote node")
    IllegalStateException failedToReceivePartitionCollectorData(@Cause Throwable cause);


}
