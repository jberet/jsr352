/*
 * Copyright (c) 2026 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.se.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.batch.operations.JobOperator;
import jakarta.batch.operations.NoSuchJobException;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import org.jberet.operations.ExtendedJobOperator;
import org.jberet.operations.FailExecutionsResult;
import org.jberet.operations.FailJobExecutionsException;
import org.jberet.operations.FailOptions;
import org.jberet.operations.JobOperatorImpl;
import org.jberet.operations.StaleExecutionsPage;
import org.jberet.repository.FailOutcome;
import org.jberet.repository.InMemoryRepository;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.spi.JobOperatorContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

/**
 * Java SE coverage for {@link ExtendedJobOperator#listExecutionsNotUpdatedInMinutes}.
 * <p>
 * The Java SE runtime is configured for a JDBC job repository on H2
 * (see {@code src/test/resources/jberet.properties}), so these run against the real
 * {@code JdbcRepository}, and one test repeats the exercise over an {@code InMemoryRepository} to
 * show the operator behaves the same over both.
 * <p>
 * This test class uses its own job, {@code org.jberet.se.test.adminOps}. It must not start
 * {@code org.jberet.se.test.batchlet1} or its siblings: {@code JobDataTest} asserts against the
 * latest job instance {@code Batchlet1Test} leaves behind, and starting those jobs from here would
 * break it through test ordering. The job name is also distinct from the one
 * {@code FailJobExecutionsSETest} uses, because both classes share the same H2 database.
 */
public class StaleExecutionsSETest {

    private static final String JOB = "org.jberet.se.test.adminOps";
    private static final long AWAIT_SECONDS = 60;

    private final JobOperator operator = BatchRuntime.getJobOperator();

    /** Lets every parked execution finish so the JVM can exit. */
    @AfterAll
    public static void releaseParkedExecutions() {
        AdminOpsTargetBatchlet.release.countDown();
    }

    private static Properties blocking() {
        final Properties params = new Properties();
        params.setProperty("block", "true");
        return params;
    }

    /**
     * Starts {@code n} executions that park, and returns once every one of them is genuinely
     * running, so the job repository really does hold {@code n} {@code STARTED} executions.
     */
    private List<Long> startParkedExecutions(final ExtendedJobOperator admin, final int n) throws Exception {
        AdminOpsTargetBatchlet.running = new CountDownLatch(n);
        final List<Long> started = new ArrayList<Long>(n);
        for (int i = 0; i < n; i++) {
            started.add(admin.start(JOB, blocking()));
        }
        assertTrue(AdminOpsTargetBatchlet.running.await(AWAIT_SECONDS, TimeUnit.SECONDS),
                "the executions should all have reached the batchlet");
        //the staleness predicate is strictly older than the cut-off, so let the clock move past
        //the progress update the runtime just wrote
        Thread.sleep(50);
        return started;
    }

    // ---------------------------------------------------------------------------------------
    // reachability and dependency hygiene
    // ---------------------------------------------------------------------------------------

    /**
     * The documented way to get at these operations is a cast on the operator the Jakarta Batch
     * runtime hands out, so that cast has to work.
     */
    @Test
    public void extendedOperatorIsReachableFromBatchRuntime() {
        assertInstanceOf(ExtendedJobOperator.class, BatchRuntime.getJobOperator());
        assertInstanceOf(ExtendedJobOperator.class,
                JobOperatorContext.getJobOperatorContext().getJobOperator());
    }

    /**
     * The public API must stay loadable anywhere JBeret runs, which means it must not reach for
     * anything outside the JDK, {@code jakarta.batch} and JBeret itself.
     * <p>
     * The check reads each class file's constant pool, so it sees every type the compiled class
     * actually mentions, not only the ones in an import statement. It is a direct-reference check:
     * these types do depend on {@code org.jberet._private.BatchMessages} for their localised
     * strings, and that in turn uses {@code org.jboss.logging}, which every class in
     * {@code jberet-core} already does.
     */
    @Test
    public void apiTypesReferenceOnlyJdkJakartaBatchAndJBeret() throws Exception {
        final Class<?>[] apiTypes = {
                ExtendedJobOperator.class,
                StaleExecutionsPage.class,
                FailExecutionsResult.class,
                FailExecutionsResult.SkippedExecution.class,
                FailExecutionsResult.SkipReason.class,
                FailOptions.class,
                FailJobExecutionsException.class,
                FailOutcome.class,
                FailOutcome.Kind.class,
        };
        final Set<String> offenders = new TreeSet<String>();
        for (final Class<?> apiType : apiTypes) {
            for (final String referenced : referencedClassNames(apiType)) {
                if (!referenced.startsWith("java.")
                        && !referenced.startsWith("jakarta.batch.")
                        && !referenced.startsWith("org.jberet.")) {
                    offenders.add(apiType.getSimpleName() + " -> " + referenced);
                }
            }
        }
        assertTrue(offenders.isEmpty(), "API types must not reference anything else: " + offenders);
    }

    /** Reads every class name a compiled class mentions, from its constant pool. */
    private static Set<String> referencedClassNames(final Class<?> type) throws IOException {
        final String resource = '/' + type.getName().replace('.', '/') + ".class";
        final Set<String> utf8 = new LinkedHashSet<String>();
        final List<Integer> classNameIndexes = new ArrayList<Integer>();
        final List<String> constantPool = new ArrayList<String>();
        constantPool.add(null);     //the constant pool is 1-based

        try (InputStream resourceStream = type.getResourceAsStream(resource)) {
            if (resourceStream == null) {
                fail("cannot read the class file for " + type.getName());
            }
            final DataInputStream in = new DataInputStream(resourceStream);
            in.readInt();           //magic
            in.readUnsignedShort(); //minor
            in.readUnsignedShort(); //major
            final int poolCount = in.readUnsignedShort();
            for (int i = 1; i < poolCount; i++) {
                final int tag = in.readUnsignedByte();
                switch (tag) {
                    case 1:         //Utf8
                        final String value = in.readUTF();
                        constantPool.add(value);
                        utf8.add(value);
                        break;
                    case 7:         //Class
                    case 19:        //Module
                    case 20:        //Package
                        classNameIndexes.add(in.readUnsignedShort());
                        constantPool.add(null);
                        break;
                    case 8:         //String
                    case 16:        //MethodType
                        in.readUnsignedShort();
                        constantPool.add(null);
                        break;
                    case 15:        //MethodHandle
                        in.readUnsignedByte();
                        in.readUnsignedShort();
                        constantPool.add(null);
                        break;
                    case 5:         //Long
                    case 6:         //Double
                        in.readLong();
                        constantPool.add(null);
                        constantPool.add(null);
                        i++;        //takes two pool slots
                        break;
                    default:        //Integer, Float, Fieldref, Methodref, NameAndType, Dynamic, ...
                        in.readInt();
                        constantPool.add(null);
                        break;
                }
            }
        }

        final Set<String> referenced = new HashSet<String>();
        for (final Integer index : classNameIndexes) {
            final String name = index < constantPool.size() ? constantPool.get(index) : null;
            if (name != null && !name.startsWith("[")) {
                referenced.add(name.replace('/', '.'));
            }
        }
        //descriptors mention types that never appear as a CONSTANT_Class, e.g. method parameters
        final Pattern descriptorType = Pattern.compile("L([a-zA-Z0-9_$/]+);");
        for (final String value : utf8) {
            final Matcher matcher = descriptorType.matcher(value);
            while (matcher.find()) {
                referenced.add(matcher.group(1).replace('/', '.'));
            }
        }
        return referenced;
    }

    // ---------------------------------------------------------------------------------------
    // listing against the JDBC job repository
    // ---------------------------------------------------------------------------------------

    @Test
    public void listsRunningExecutionsWhenMinutesIsZero() throws Exception {
        final ExtendedJobOperator admin = (ExtendedJobOperator) operator;
        final List<Long> started = startParkedExecutions(admin, 3);

        final StaleExecutionsPage page = admin.listExecutionsNotUpdatedInMinutes(JOB, 0, 0, 100);

        assertTrue(page.getExecutionIds().containsAll(started),
                "expected " + page.getExecutionIds() + " to contain " + started);
        assertTrue(page.getTotalCount() >= started.size());
        assertEquals(0, page.getStart());
        assertEquals(page.getExecutionIds().size(), page.getCount());
        //ascending order makes paging deterministic
        final List<Long> sorted = new ArrayList<Long>(page.getExecutionIds());
        Collections.sort(sorted);
        assertEquals(sorted, page.getExecutionIds());
    }

    /** An execution that started moments ago is not stale by an hour's measure. */
    @Test
    public void doesNotListRecentlyUpdatedExecutions() throws Exception {
        final ExtendedJobOperator admin = (ExtendedJobOperator) operator;
        final List<Long> started = startParkedExecutions(admin, 1);

        final StaleExecutionsPage page = admin.listExecutionsNotUpdatedInMinutes(JOB, 60, 0, 100);

        for (final Long id : started) {
            assertFalse(page.getExecutionIds().contains(id),
                    "execution " + id + " started moments ago and must not count as an hour stale");
        }
    }

    /** Walking the pages visits every matching execution exactly once. */
    @Test
    public void pagesWithoutGapsOrDuplicates() throws Exception {
        final ExtendedJobOperator admin = (ExtendedJobOperator) operator;
        final List<Long> started = startParkedExecutions(admin, 5);

        final StaleExecutionsPage first = admin.listExecutionsNotUpdatedInMinutes(JOB, 0, 0, 2);
        final long total = first.getTotalCount();
        assertTrue(total >= 5, "expected at least the 5 executions just started, got " + total);
        assertEquals(2, first.getCount());
        assertTrue(first.hasMore());

        final List<Long> walked = new ArrayList<Long>(first.getExecutionIds());
        for (int start = 2; start < total; start += 2) {
            walked.addAll(admin.listExecutionsNotUpdatedInMinutes(JOB, 0, start, 2).getExecutionIds());
        }

        assertEquals(walked.size(), new LinkedHashSet<Long>(walked).size(), "no id should repeat: " + walked);
        assertTrue(walked.containsAll(started), "expected " + walked + " to contain " + started);

        //a page past the end is empty but still reports the total
        final StaleExecutionsPage beyond =
                admin.listExecutionsNotUpdatedInMinutes(JOB, 0, (int) total + 100, 10);
        assertTrue(beyond.getExecutionIds().isEmpty());
        assertFalse(beyond.hasMore());
    }

    @Test
    public void unknownJobIsRejected() {
        final ExtendedJobOperator admin = (ExtendedJobOperator) operator;

        assertThrows(NoSuchJobException.class,
                () -> admin.listExecutionsNotUpdatedInMinutes("org.jberet.se.test.noSuchJob", 0, 0, 10));
        assertThrows(NoSuchJobException.class,
                () -> admin.listExecutionsNotUpdatedInMinutes(null, 0, 0, 10));
    }

    @Test
    public void outOfRangeArgumentsAreRejected() {
        final ExtendedJobOperator admin = (ExtendedJobOperator) operator;

        assertThrows(IllegalArgumentException.class,
                () -> admin.listExecutionsNotUpdatedInMinutes(JOB, -1, 0, 10));
        assertThrows(IllegalArgumentException.class,
                () -> admin.listExecutionsNotUpdatedInMinutes(JOB, 0, -1, 10));
        assertThrows(IllegalArgumentException.class,
                () -> admin.listExecutionsNotUpdatedInMinutes(JOB, 0, 0, 0));
    }

    // ---------------------------------------------------------------------------------------
    // the same operations over an in-memory job repository
    // ---------------------------------------------------------------------------------------

    /**
     * Runs the whole list-then-fail cycle over an {@code InMemoryRepository} in the same Java SE
     * runtime, to show the operator's behaviour does not depend on having a database behind it.
     */
    @Test
    public void worksOverAnInMemoryRepository() throws Exception {
        final InMemoryRepository repository = InMemoryRepository.create();
        final ExtendedJobOperator admin = new JobOperatorImpl(
                AdminOpsFixtures.withRepository(AdminOpsFixtures.seEnvironment(), repository));

        AdminOpsTargetBatchlet.running = new CountDownLatch(1);
        final long executionId = admin.start(JOB, blocking());
        assertTrue(AdminOpsTargetBatchlet.running.await(AWAIT_SECONDS, TimeUnit.SECONDS));
        Thread.sleep(50);

        final StaleExecutionsPage page = admin.listExecutionsNotUpdatedInMinutes(JOB, 0, 0, 10);
        assertEquals(Collections.singletonList(executionId), page.getExecutionIds());
        assertEquals(1L, page.getTotalCount());

        //it is running right here, so the liveness screen must refuse to fail it
        final FailExecutionsResult screened = admin.failJobExecutions(page.getExecutionIds());
        assertEquals(0, screened.getFailedCount());
        assertEquals(FailExecutionsResult.SkipReason.RUNNING_LOCALLY,
                screened.getSkipped().get(0).getReason());

        //with the screen off it is failed, and the change is visible through the operator
        final FailExecutionsResult failed = admin.failJobExecutions(page.getExecutionIds(),
                FailOptions.defaults().withSkipLocallyRunning(false));
        assertEquals(1, failed.getFailedCount());
        assertEquals(BatchStatus.FAILED,
                ((JobExecutionImpl) admin.getJobExecution(executionId)).getBatchStatus());
        assertEquals(ExtendedJobOperator.FAILED_BY_ADMIN, admin.getJobExecution(executionId).getExitStatus());

        //and it is no longer stale, because it is no longer running
        assertTrue(admin.listExecutionsNotUpdatedInMinutes(JOB, 0, 0, 10).getExecutionIds().isEmpty());
    }

    /*This test will need to be removed once MongoDB and Infinispan repository are updated*/
    /**
     * MongoDB and Infinispan ship without implementations of these operations for now, so they must
     * inherit {@code JobRepository}'s throwing defaults rather than silently doing something
     * approximate. Checking that they declare no override is the cheap, exact form of that
     * assertion: it needs no running MongoDB or Infinispan, and it fails the moment someone adds a
     * half-finished implementation without also updating the documentation.
     */
    @Test
    public void mongoAndInfinispanRepositoriesDoNotSupportTheseOperations() {
        final Class<?>[] unsupported = {
                org.jberet.repository.MongoRepository.class,
                org.jberet.repository.InfinispanRepository.class,
        };
        for (final Class<?> repositoryType : unsupported) {
            assertFalse(declares(repositoryType, "countStaleExecutions", String.class, java.util.Date.class),
                    repositoryType.getSimpleName() + " unexpectedly implements countStaleExecutions");
            assertFalse(declares(repositoryType, "findStaleExecutionIds", String.class, java.util.Date.class,
                            int.class, int.class),
                    repositoryType.getSimpleName() + " unexpectedly implements findStaleExecutionIds");
            assertFalse(declares(repositoryType, "failJobExecutions", java.util.Collection.class, String.class,
                            String.class, java.util.Date.class),
                    repositoryType.getSimpleName() + " unexpectedly implements failJobExecutions");
        }
    }

    /** True when the type, or any supertype below {@code JobRepository}, declares the method. */
    private static boolean declares(final Class<?> type, final String name, final Class<?>... parameterTypes) {
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                c.getDeclaredMethod(name, parameterTypes);
                return true;
            } catch (final NoSuchMethodException ignored) {
                //keep walking up
            }
        }
        return false;
    }
}
