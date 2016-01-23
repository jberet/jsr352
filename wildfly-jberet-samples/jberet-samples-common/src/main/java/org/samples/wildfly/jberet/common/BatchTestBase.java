package org.samples.wildfly.jberet.common;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import javax.batch.runtime.BatchStatus;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import org.junit.Assert;

/**
 * Base batch test class to be extended by more concrete batch test cases.
 *
 * @author Cheng Fang
 */
public abstract class BatchTestBase {
    private static final String BASE_URL = "http://localhost:8080";

    protected TextPage runJob(final String contextPath,
                              final String servletPath,
                              final String jobCommand,
                              final NameValuePair... requestParams) throws Exception {
        if (jobCommand == null || jobCommand.isEmpty()) {
            throw new IllegalArgumentException(BatchJobServlet.JOB_COMMAND_KEY + " must be a non-empty string");
        }
        final WebClient webClient = new WebClient();
        String url = BASE_URL;
        if (contextPath != null && !contextPath.isEmpty()) {
            url += (contextPath.startsWith("/")) ? contextPath : "/" + contextPath;
        }
        if (servletPath != null && !servletPath.isEmpty()) {
            url += (servletPath.startsWith("/")) ? servletPath : "/" + servletPath;
        }
        final WebRequest request = new WebRequest(new URI(url).toURL(), HttpMethod.POST);
        final List<NameValuePair> paramsList = new ArrayList<NameValuePair>();
        paramsList.add(new NameValuePair(BatchJobServlet.JOB_COMMAND_KEY, jobCommand));

        Collections.addAll(paramsList, requestParams);
        request.setRequestParameters(paramsList);
        return webClient.getPage(request);
    }

    /**
     * Asserts that the content contains each element of statuses, and that other {@code BatchStatus} values
     * are not contained in the content.
     * @param content the string content to verify
     * @param statuses one or more {@code BatchStatus} enum values
     */
    protected void assertContainsBatchStatus(final String content, final BatchStatus... statuses) {
        System.out.println(content);
        final HashSet<BatchStatus> batchStatuses = new HashSet<BatchStatus>();
        Collections.addAll(batchStatuses, statuses);
        for (final BatchStatus status : BatchStatus.values()) {
            if (batchStatuses.contains(status)) {
                Assert.assertEquals(true, content.contains(status.toString()));
            } else {
                Assert.assertEquals(false, content.contains(status.toString()));
            }
        }
    }
}
