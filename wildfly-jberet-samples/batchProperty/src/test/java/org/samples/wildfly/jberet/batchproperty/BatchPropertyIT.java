package org.samples.wildfly.jberet.batchproperty;

import javax.batch.runtime.BatchStatus;

import com.gargoylesoftware.htmlunit.TextPage;
import org.junit.Test;
import org.samples.wildfly.jberet.common.BatchTestBase;

/**
 * Test class that connects to the test servlet to start the batch job.
 *
 * @author Cheng Fang
 */
public class BatchPropertyIT extends BatchTestBase {
    static final String CONTEXT_PATH = "batchproperty";
    static final String SERVLET_PATH = null;
    static final String JOB_COMMAND = "start " + CONTEXT_PATH;

    @Test
    public void testBatchProperty() throws Exception {
        final TextPage page = runJob(CONTEXT_PATH, SERVLET_PATH, JOB_COMMAND);
        final String content = page.getContent();
        assertContainsBatchStatus(content, BatchStatus.COMPLETED);
    }
}
