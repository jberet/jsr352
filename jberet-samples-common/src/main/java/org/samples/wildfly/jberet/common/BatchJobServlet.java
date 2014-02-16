package org.samples.wildfly.jberet.common;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.StepExecution;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jberet.runtime.JobExecutionImpl;

/**
 * Servlet that starts a batch job, and waits for job execution result.
 *
 * @author Cheng Fang
 */

@WebServlet(urlPatterns = "/*")
public class BatchJobServlet extends HttpServlet {
    static final String JOB_NAME_KEY = "jobName";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        final String jobName = request.getParameter(JOB_NAME_KEY);
        final Properties jobParams = new Properties();
        final Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            final String key = parameterNames.nextElement();
            final String val = request.getParameter(key);
            if (val != null && !val.isEmpty()) {
                jobParams.setProperty(key, val);
            }
        }

        final JobOperator jobOperator = BatchRuntime.getJobOperator();
        final long jobExecutionId = jobOperator.start(jobName, jobParams);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        try {
            jobExecution.awaitTermination(0, TimeUnit.MINUTES);
        } catch (final InterruptedException e) {
            throw new ServletException(e);
        }
        final PrintWriter pw = response.getWriter();
        pw.println(JOB_NAME_KEY + ": " + jobName);
        pw.println("job parameters: " + jobParams);
        pw.println("job batch status: " + jobExecution.getBatchStatus());
        pw.println("job exit status: " + jobExecution.getExitStatus());

        final List<StepExecution> stepExecutions = jobOperator.getStepExecutions(jobExecutionId);
        for (final StepExecution stepExecution : stepExecutions) {
            pw.println("step: " + stepExecution.getStepName() +
                    ", batch status: " + stepExecution.getBatchStatus() +
                    ", exit status: " + stepExecution.getExitStatus());
        }
    }
}
