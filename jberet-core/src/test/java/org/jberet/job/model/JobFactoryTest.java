package org.jberet.job.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.jberet.creation.ArchiveXmlLoader;
import org.jberet.tools.MetaInfBatchJobsJobXmlResolver;
import org.jboss.marshalling.cloner.ClonerConfiguration;
import org.jboss.marshalling.cloner.ObjectCloner;
import org.jboss.marshalling.cloner.ObjectCloners;
import org.junit.Test;

public class JobFactoryTest {
    private static final ObjectCloner cloner =
        ObjectCloners.getSerializingObjectClonerFactory().createCloner(new ClonerConfiguration());

    @Test
    public void cloneJob() {
        Job original = loadJob("sample-job.xml");
        Job clone = JobFactory.cloneJob(original);

        assertJob(original, clone);
    }

    @Test
    public void cloneJobWithCloner() throws Exception {
        Job original = loadJob("sample-job.xml");
        Job clone = (Job) cloner.clone(original);

        assertJob(original, clone);
    }

    @Test
    public void compareClones() throws Exception {
        Job original = loadJob("sample-job.xml");

        Job oldClone = (Job) cloner.clone(original);
        Job newClone = JobFactory.cloneJob(original);

        assertJob(oldClone, newClone);
    }

    private static void assertJob(Job original, Job clone) {
        assertNotSame(original, clone);
        assertEquals(original.getId(), clone.getId());
        assertEquals(original.getJobXmlName(), clone.getJobXmlName());
        assertEquals(original.getRestartable(), clone.getRestartable());

        assertInheritableJobElement(original, clone);
        assertJobElements(original.getJobElements(), clone.getJobElements());
        assertInheritableJobElements(original.getInheritingJobElements(), clone.getInheritingJobElements());

        assertEquals(original.getTransitionElements(), clone.getTransitionElements());
    }

    private static void assertJobElements(List<JobElement> originals, List<JobElement> clones) {
        assertNotSame(originals, clones);
        assertEquals(originals.size(), clones.size());

        for (int i = 0; i < originals.size(); i++) {
            assertJobElement(originals.get(i), clones.get(i));
        }
    }

    private static void assertJobElement(JobElement original, JobElement clone) {
        assertAbstractJobElement((AbstractJobElement) original, (AbstractJobElement) clone);

        if (original instanceof Step) {
            assertStep((Step) original, (Step) clone);
        }

        if (original instanceof Flow) {
            assertFlow((Flow) original, (Flow) clone);
        }

        if (original instanceof Decision) {
            assertDecision((Decision) original, (Decision) clone);
        }

        if (original instanceof Split) {
            assertSplit((Split) original, (Split) clone);
        }
    }

    private static void assertAbstractJobElement(AbstractJobElement original, AbstractJobElement clone) {
        assertEquals(original.getId(), clone.getId());
        assertProperties(original.getProperties(), clone.getProperties());

        assertNotSame(original.getTransitionElements(), clone.getTransitionElements());
        List<Transition> originalTransitions = original.getTransitionElements();
        List<Transition> cloneTransitions = original.getTransitionElements();
        for (int i = 0; i < originalTransitions.size(); i++) {
            Transition originalTransition = originalTransitions.get(i);
            Transition clonedTransition = cloneTransitions.get(i);
            assertSame(originalTransition, clonedTransition);
            assertEquals(originalTransition.getOn(), clonedTransition.getOn());
        }
    }

    private static void assertInheritableJobElements(List<InheritableJobElement> originals, List<InheritableJobElement> clones) {
        assertNotSame(originals, clones);
        assertEquals(originals.size(), clones.size());

        for (int i = 0; i < originals.size(); i++) {
            assertInheritableJobElement(originals.get(i), clones.get(i));
        }
    }

    private static void assertInheritableJobElement(InheritableJobElement original, InheritableJobElement clone) {
        assertEquals(original.getId(), clone.getId());
        assertProperties(original.getProperties(), clone.getProperties());

        if (original instanceof Flow) {
            assertNull(original.getListeners());
            assertNull(clone.getListeners());
            return;
        }

        assertNotSame(original.getListeners(), clone.getListeners());
        assertNotSame(original.getListeners().getListeners(), clone.getListeners().getListeners());
        List<RefArtifact> originalListeners = original.getListeners().getListeners();
        List<RefArtifact> cloneListeners = clone.getListeners().getListeners();
        for (int i = 0; i < originalListeners.size(); i++) {
            assertRefArtifact(originalListeners.get(i), cloneListeners.get(i));
        }

        assertEquals(original.isAbstract(), clone.isAbstract());
        assertEquals(original.getParent(), clone.getParent());
        assertEquals(original.getJslName(), clone.getJslName());
    }

    private static void assertStep(Step original, Step clone) {
        assertNotSame(original, clone);
        assertEquals(original.getId(), clone.getId());
        assertEquals(original.getStartLimit(), clone.getStartLimit());
        assertEquals(original.getAllowStartIfComplete(), clone.getAllowStartIfComplete());
        assertEquals(original.getAttributeNext(), clone.getAttributeNext());

        assertRefArtifact(original.getBatchlet(), clone.getBatchlet());
        assertChunk(original.getChunk(), clone.getChunk());
        assertPartition(original.getPartition(), clone.getPartition());

        assertInheritableJobElement(original, clone);
    }

    private static void assertRefArtifact(RefArtifact original, RefArtifact clone) {
        if (original == null && clone == null) {
            return;
        }
        assertNotNull(original);
        assertNotNull(clone);
        assertNotSame(original, clone);

        assertEquals(original.getRef(), clone.getRef());
        assertEquals(original.getScript(), clone.getScript());
        assertProperties(original.getProperties(), clone.getProperties());
    }

    private static void assertChunk(Chunk original, Chunk clone) {
        if (original == null && clone == null) {
            return;
        }
        assertNotNull(original);
        assertNotNull(clone);
        assertNotSame(original, clone);

        assertRefArtifact(original.getReader(), clone.getReader());
        assertRefArtifact(original.getProcessor(), clone.getProcessor());
        assertRefArtifact(original.getWriter(), clone.getWriter());
        assertRefArtifact(original.getCheckpointAlgorithm(), clone.getCheckpointAlgorithm());
        assertExceptionClassFilter(original.getSkippableExceptionClasses(), clone.getSkippableExceptionClasses());
        assertExceptionClassFilter(original.getRetryableExceptionClasses(), clone.getRetryableExceptionClasses());
        assertExceptionClassFilter(original.getNoRollbackExceptionClasses(), clone.getNoRollbackExceptionClasses());
        assertEquals(original.getCheckpointPolicy(), clone.getCheckpointPolicy());
        assertEquals(original.getItemCount(), clone.getItemCount());
        assertEquals(original.getTimeLimit(), clone.getTimeLimit());
        assertEquals(original.getSkipLimit(), clone.getSkipLimit());
        assertEquals(original.getRetryLimit(), clone.getRetryLimit());
    }

    private static void assertExceptionClassFilter(ExceptionClassFilter original, ExceptionClassFilter clone) {
        if (original == null && clone == null) {
            return;
        }
        assertNotNull(original);
        assertNotNull(clone);
        assertNotSame(original, clone);

        assertNotSame(original.getInclude(), clone.getInclude());
        assertEquals(original.getInclude(), clone.getInclude());
        assertNotSame(original.getExclude(), clone.getExclude());
        assertEquals(original.getExclude(), clone.getExclude());
    }

    private static void assertPartition(Partition original, Partition clone) {
        if (original == null && clone == null) {
            return;
        }
        assertNotNull(original);
        assertNotNull(clone);
        assertNotSame(original, clone);

        assertRefArtifact(original.getMapper(), clone.getMapper());
        assertRefArtifact(original.getCollector(), clone.getCollector());
        assertRefArtifact(original.getAnalyzer(), clone.getAnalyzer());
        assertRefArtifact(original.getReducer(), clone.getReducer());

        if (original.getPlan() == null && clone.getPlan() == null) {
            return;
        }
        assertNotNull(original.getPlan());
        assertNotNull(clone.getPlan());
        assertNotSame(original, clone);

        assertEquals(original.getPlan().getPartitions(), clone.getPlan().getPartitions());
        assertEquals(original.getPlan().getThreads(), clone.getPlan().getThreads());
        assertNotSame(original.getPlan().getPropertiesList(), clone.getPlan().getPropertiesList());

        List<Properties> originalProperties = original.getPlan().getPropertiesList();
        List<Properties> cloneProperties = clone.getPlan().getPropertiesList();
        for (int i = 0; i < originalProperties.size(); i++) {
            assertProperties(originalProperties.get(i), cloneProperties.get(i));
        }
    }

    private static void assertFlow(Flow original, Flow clone) {
        if (original == null && clone == null) {
            return;
        }
        assertNotNull(original);
        assertNotNull(clone);
        assertNotSame(original, clone);

        assertEquals(original.getAttributeNext(), clone.getAttributeNext());
        assertInheritableJobElement(original, clone);
        assertJobElements(original.getJobElements(), clone.getJobElements());
    }

    private static void assertDecision(Decision original, Decision clone) {
        if (original == null && clone == null) {
            return;
        }
        assertNotNull(original);
        assertNotNull(clone);
        assertNotSame(original, clone);

        assertEquals(original.getRef(), clone.getRef());
    }

    private static void assertSplit(Split original, Split clone) {
        if (original == null && clone == null) {
            return;
        }
        assertNotNull(original);
        assertNotNull(clone);
        assertNotSame(original, clone);

        assertEquals(original.getAttributeNext(), clone.getAttributeNext());
        List<Flow> originalFlows = original.getFlows();
        List<Flow> cloneFlows = clone.getFlows();
        assertNotSame(originalFlows, cloneFlows);
        assertEquals(originalFlows.size(), cloneFlows.size());

        for (int i = 0; i < originalFlows.size(); i++) {
            assertJobElement(originalFlows.get(i), cloneFlows.get(i));
        }
    }

    private static void assertProperties(Properties original, Properties clone) {
        if (original == null && clone == null) {
            return;
        }
        assertNotNull(original);
        assertNotNull(clone);
        assertNotSame(original, clone);

        assertEquals(original.getPartition(), clone.getPartition());
        assertNotSame(original.getNameValues(), clone.getNameValues());
        assertEquals(original.getNameValues(), clone.getNameValues());
    }

    private static Job loadJob(final String jobName) {
        return ArchiveXmlLoader.loadJobXml(jobName, JobMergerTest.class.getClassLoader(), new ArrayList<>(), new MetaInfBatchJobsJobXmlResolver());
    }
}
