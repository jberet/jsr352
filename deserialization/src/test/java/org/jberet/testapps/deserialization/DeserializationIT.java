package org.jberet.testapps.deserialization;

import javax.batch.runtime.BatchStatus;

import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import org.junit.Ignore;
import org.junit.Test;
import org.samples.wildfly.jberet.common.BatchTestBase;

public class DeserializationIT extends BatchTestBase {
    static final String CONTEXT_PATH = "deserialization";
    static final String SERVLET_PATH = null;

    @Test
    public void startJob() throws Exception {
        final TextPage page = runJob(CONTEXT_PATH, SERVLET_PATH, "start " + CONTEXT_PATH,
                new NameValuePair("fail.on", "8"));
        final String content = page.getContent();
        assertContainsBatchStatus(content, BatchStatus.FAILED);
    }

    @Test
    @Ignore("This test bans the job to be restarted, and any restart will fail")
    public void banRestart() throws Exception {
        final TextPage page = runJob(CONTEXT_PATH, SERVLET_PATH, "start " + CONTEXT_PATH,
                new NameValuePair("restartable", Boolean.FALSE.toString()),
                new NameValuePair("fail.on", "8"));
        final String content = page.getContent();
        assertContainsBatchStatus(content, BatchStatus.FAILED);
    }

    @Test
    public void restartJob() throws Exception {
        final TextPage page = runJob(CONTEXT_PATH, SERVLET_PATH, "restart " + CONTEXT_PATH);
        final String content = page.getContent();
        assertContainsBatchStatus(content, BatchStatus.COMPLETED);
    }



    @Test
    public void startJobNameDifferent() throws Exception {
        final TextPage page = runJob(CONTEXT_PATH, SERVLET_PATH, "start job-xml-name-different",
                new NameValuePair("fail.on", "8"));
        final String content = page.getContent();
        assertContainsBatchStatus(content, BatchStatus.FAILED);
    }

    @Test
    public void restartJobNameDifferent() throws Exception {
        final TextPage page = runJob(CONTEXT_PATH, SERVLET_PATH, "restart jobXmlNameDifferent");
        final String content = page.getContent();
        assertContainsBatchStatus(content, BatchStatus.COMPLETED);
    }

}
