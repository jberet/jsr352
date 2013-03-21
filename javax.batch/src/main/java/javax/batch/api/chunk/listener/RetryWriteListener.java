package javax.batch.api.chunk.listener;

import java.util.List;

/**
 * RetryWriteListener intercepts retry processing for an ItemWriter.
 * 
 */
public interface RetryWriteListener {
    /**
     * The onRetryWriteException method receives control when a retryable
     * exception is thrown from an ItemWriter writeItems method. This method
     * receives the exception and the list of items being written as inputs.
     * This method receives control in same checkpoint scope as the ItemWriter.
     * If this method throws a an exception, the job ends in the FAILED state.
     * 
     * @param items
     *            specify the items passed to an item writer.
     * @param ex
     *            specifies the exception thrown by an item writer.
     * @throws Exception
     *             is thrown if an error occurs.
     */
    public void onRetryWriteException(List<Object> items, Exception ex) throws Exception;
}
