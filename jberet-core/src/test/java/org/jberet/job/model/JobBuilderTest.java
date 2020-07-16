package org.jberet.job.model;

import org.jberet.creation.ArchiveXmlLoader;
import org.jberet.tools.MetaInfBatchJobsJobXmlResolver;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JobBuilderTest {
    @Test
    public void jobToBuilder() {
        final Job job = loadJob("sample-job.xml");
        final JobBuilder jobBuilder = job.toBuilder();
        final Job jobClone = jobBuilder.build();

        assertEquals(job.getId(), jobClone.getId());
        assertEquals(job.getRestartableBoolean(), jobClone.getRestartableBoolean());

        assertNotNull(job.getListeners());
        assertEquals(job.getListeners().getListeners().size(), jobClone.getListeners().getListeners().size());
        for (int i = 0; i < job.getListeners().getListeners().size(); i++) {
            RefArtifact refArtifact = job.getListeners().getListeners().get(i);
            RefArtifact refArtifactClone = job.getListeners().getListeners().get(i);
            assertEquals(refArtifact.getRef(), refArtifactClone.getRef());
            assertEquals(refArtifact.getProperties(), refArtifactClone.getProperties());
            assertEquals(refArtifact.getScript(), refArtifactClone.getScript());
        }

        assertNotNull(job.getJobElements());
        //assertEquals(job.getJobElements().size(), jobClone.getJobElements().size());
        for (int i = 0; i < job.getJobElements().size(); i++) {
            JobElement jobElement = job.getJobElements().get(i);
            JobElement jobElementClone = job.getJobElements().get(i);
            assertEquals(jobElement.getId(), jobElementClone.getId());

            if (jobElement instanceof Step) {
                Step step = (Step) jobElement;
                Step stepClone = (Step) jobElementClone;
                assertEquals(step.getProperties(), stepClone.getProperties());
                assertEquals(step.getStartLimit(), stepClone.getStartLimit());

                if (step.getBatchlet() != null) {
                    assertEquals(step.getBatchlet().getRef(), stepClone.getBatchlet().getRef());
                    assertEquals(step.getBatchlet().getProperties(), stepClone.getBatchlet().getProperties());
                    assertEquals(step.getBatchlet().getScript(), stepClone.getBatchlet().getScript());
                }
            }
        }
    }

    private static Job loadJob(final String jobName) {
        return ArchiveXmlLoader.loadJobXml(jobName, JobMergerTest.class.getClassLoader(), new ArrayList<>(), new MetaInfBatchJobsJobXmlResolver());
    }
}
