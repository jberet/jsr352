package org.samples.wildfly.jberet.common;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
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
    static final String JOB_COMMAND_KEY = "jobCommand";

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
        String jobCommand = request.getParameter(JOB_COMMAND_KEY);
        final String[] jobCommandLine = jobCommand.split(" ");

        final Properties jobParams = new Properties();
        final Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            final String key = parameterNames.nextElement();
            final String val = request.getParameter(key);
            if (val != null && !val.isEmpty()) {
                jobParams.setProperty(key, val);
            }
        }

        final PrintWriter pw = response.getWriter();
        pw.println(JOB_COMMAND_KEY + ": " + jobCommand);
        pw.println("job parameters: " + jobParams);

        final JobOperator jobOperator = BatchRuntime.getJobOperator();

        long jobExecutionId;
        final String jobCommandName = jobCommandLine[0];
        if (jobCommandName.equals("start")) {
            jobExecutionId = jobOperator.start(jobCommandLine[1], jobParams);
        } else if (jobCommandName.equals("restart")) {
            final String jobName = jobCommandLine[1];
            final List<JobInstance> jobInstances = jobOperator.getJobInstances(jobName, 0, 1);
            final List<JobExecution> jobExecutions = jobOperator.getJobExecutions(jobInstances.get(0));
            jobExecutionId = jobOperator.restart(jobExecutions.get(0).getExecutionId(), jobParams);
        } else {
            throw new ServletException(JOB_COMMAND_KEY + " not implemented yet: " + jobCommand);
        }

        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        try {
            jobExecution.awaitTermination(5, TimeUnit.MINUTES);
        } catch (final InterruptedException e) {
            throw new ServletException(e);
        }

        pw.println("job name: " + jobExecution.getJobName() + ", job execution id: " + jobExecutionId);
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
