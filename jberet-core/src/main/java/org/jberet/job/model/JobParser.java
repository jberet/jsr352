/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.job.model;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jberet.util.BatchLogger;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

/**
 * StAX parser for batch job xml and batch xml files.
 */
public final class JobParser {
    private static final String namespaceURI = null;

    /**
     * Parses a job xml file, which defines a batch job.
     *
     * @param inputStream the source of the job xml definition
     * @return a Job object
     * @throws XMLStreamException
     */
    public static Job parseJob(final InputStream inputStream) throws XMLStreamException {
        final XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(inputStream);
        Job job = null;
        try {
            while (reader.hasNext()) {
                final int eventType = reader.next();
                if (eventType != START_ELEMENT && eventType != END_ELEMENT) {
                    continue;
                }
                final XmlElement element = XmlElement.forName(reader.getLocalName());
                switch (eventType) {
                    case START_ELEMENT:
                        switch (element) {
                            case JOB:
                                job = new Job(getAttributeValue(reader, XmlAttribute.ID));
                                job.setRestartable(getAttributeValue(reader, XmlAttribute.RESTARTABLE));
                                break;
                            case STEP:
                                job.addJobElement(parseStep(reader));
                                break;
                            case FLOW:
                                job.addJobElement(parseFlow(reader));
                                break;
                            case SPLIT:
                                job.addJobElement(parseSplit(reader));
                                break;
                            case DECISION:
                                job.addJobElement(parseDecision(reader));
                                break;
                            case PROPERTIES:
                                job.setProperties(parseProperties(reader));
                                break;
                            case LISTENERS:
                                job.addListeners(parseListeners(reader));
                                break;
                            default:
                                throw BatchLogger.LOGGER.unexpectedXmlElement(element.getLocalName(), reader.getLocation());
                        }
                        break;
                    case END_ELEMENT:
                        if (element != XmlElement.JOB) {
                            throw BatchLogger.LOGGER.unexpectedXmlElement(element.getLocalName(), reader.getLocation());
                        }
                }
            }
        } finally {
            reader.close();
        }
        return job;
    }

    /**
     * Parses batch.xml, which declares batch artifacts in xml format.
     *
     * @param inputStream source of the batch.xml
     * @return a BatchArtifacts object
     * @throws XMLStreamException
     */
    public static BatchArtifacts parseBatchArtifacts(final InputStream inputStream) throws XMLStreamException {
        final XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(inputStream);
        BatchArtifacts batchArtifacts = null;
        try {
            while (reader.hasNext()) {
                final int eventType = reader.next();
                if (eventType != START_ELEMENT && eventType != END_ELEMENT) {
                    continue;
                }
                final XmlElement element = XmlElement.forName(reader.getLocalName());
                switch (eventType) {
                    case START_ELEMENT:
                        switch (element) {
                            case BATCH_ARTIFACTS:
                                batchArtifacts = new BatchArtifacts();
                                break;
                            case REF:
                                batchArtifacts.addRef(getAttributeValue(reader, XmlAttribute.ID), getAttributeValue(reader, XmlAttribute.CLASS));
                                break;
                            default:
                                throw BatchLogger.LOGGER.unexpectedXmlElement(element.getLocalName(), reader.getLocation());
                        }
                        break;
                    case END_ELEMENT:
                        if (element != XmlElement.BATCH_ARTIFACTS && element != XmlElement.REF) {
                            throw BatchLogger.LOGGER.unexpectedXmlElement(element.getLocalName(), reader.getLocation());
                        }
                }
            }
        } finally {
            reader.close();
        }
        return batchArtifacts;
    }

    private static Step parseStep(final XMLStreamReader reader) throws XMLStreamException {
        final Step step = new Step(getAttributeValue(reader, XmlAttribute.ID));
        step.setStartLimit(getAttributeValue(reader, XmlAttribute.START_LIMIT));
        step.setAllowStartIfComplete(getAttributeValue(reader, XmlAttribute.ALLOW_START_IF_COMPLETE));
        step.setAttributeNext(getAttributeValue(reader, XmlAttribute.NEXT));

        while (reader.hasNext()) {
            final int eventType = reader.next();
            if (eventType != START_ELEMENT && eventType != END_ELEMENT) {
                continue;
            }
            final XmlElement element = XmlElement.forName(reader.getLocalName());
            switch (eventType) {
                case START_ELEMENT:
                    switch (element) {
                        case PROPERTIES:
                            step.setProperties(parseProperties(reader));
                            break;
                        case LISTENERS:
                            step.addListeners(parseListeners(reader));
                            break;
                        case BATCHLET:
                            step.setBatchlet(parseRefArtifact(reader, XmlElement.BATCHLET));
                            break;
                        case CHUNK:
                            step.setChunk(parseChunk(reader));
                            break;
                        case PARTITION:
                            step.setPartition(parsePartition(reader));
                            break;
                        case NEXT:
                            step.addTransitionElement(parseNext(reader));
                            break;
                        case FAIL:
                            step.addTransitionElement(parseFail(reader));
                            break;
                        case END:
                            step.addTransitionElement(parseEnd(reader));
                            break;
                        case STOP:
                            step.addTransitionElement(parseStop(reader));
                            break;
                        default:
                            throw BatchLogger.LOGGER.unexpectedXmlElement(element.getLocalName(), reader.getLocation());
                    }
                    break;
                case END_ELEMENT:
                    switch (element) {
                        case STEP:
                            return step;
                        default:
                            throw BatchLogger.LOGGER.unexpectedXmlElement(element.getLocalName(), reader.getLocation());
                    }
            }
        }
        throw BatchLogger.LOGGER.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
    }

    private static String getAttributeValue(final XMLStreamReader reader, final XmlAttribute attribute) {
        return reader.getAttributeValue(namespaceURI, attribute.getLocalName());
    }

    private static Decision parseDecision(final XMLStreamReader reader) throws XMLStreamException {
        final Decision decision = new Decision(getAttributeValue(reader, XmlAttribute.ID), getAttributeValue(reader, XmlAttribute.REF));
        while (reader.hasNext()) {
            final int eventType = reader.next();
            if (eventType != START_ELEMENT && eventType != END_ELEMENT) {
                continue;
            }
            final XmlElement element = XmlElement.forName(reader.getLocalName());
            switch (eventType) {
                case START_ELEMENT:
                    switch (element) {
                        case PROPERTIES:
                            decision.setProperties(parseProperties(reader));
                            break;
                        case NEXT:
                            decision.addTransitionElement(parseNext(reader));
                            break;
                        case FAIL:
                            decision.addTransitionElement(parseFail(reader));
                            break;
                        case END:
                            decision.addTransitionElement(parseEnd(reader));
                            break;
                        case STOP:
                            decision.addTransitionElement(parseStop(reader));
                            break;
                        default:
                            throw BatchLogger.LOGGER.unexpectedXmlElement(element.getLocalName(), reader.getLocation());
                    }
                    break;
                case END_ELEMENT:
                    switch (element) {
                        case DECISION:
                            return decision;
                        default:
                            throw BatchLogger.LOGGER.unexpectedXmlElement(element.getLocalName(), reader.getLocation());
                    }
            }
        }
        throw BatchLogger.LOGGER.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
    }

    private static Flow parseFlow(final XMLStreamReader reader) throws XMLStreamException {
        final Flow flow = new Flow(getAttributeValue(reader, XmlAttribute.ID));
        flow.setAttributeNext(getAttributeValue(reader, XmlAttribute.NEXT));
        while (reader.hasNext()) {
            final int eventType = reader.next();
            if (eventType != START_ELEMENT && eventType != END_ELEMENT) {
                continue;
            }
            final XmlElement element = XmlElement.forName(reader.getLocalName());
            switch (eventType) {
                case START_ELEMENT:
                    switch (element) {
                        case DECISION:
                            flow.addJobElement(parseDecision(reader));
                            break;
                        case FLOW:
                            flow.addJobElement(parseFlow(reader));
                            break;
                        case SPLIT:
                            flow.addJobElement(parseSplit(reader));
                            break;
                        case STEP:
                            flow.addJobElement(parseStep(reader));
                            break;
                        case NEXT:
                            flow.addTransitionElement(parseNext(reader));
                            break;
                        case FAIL:
                            flow.addTransitionElement(parseFail(reader));
                            break;
                        case END:
                            flow.addTransitionElement(parseEnd(reader));
                            break;
                        case STOP:
                            flow.addTransitionElement(parseStop(reader));
                            break;
                        default:
                            throw BatchLogger.LOGGER.unexpectedXmlElement(element.getLocalName(), reader.getLocation());
                    }
                    break;
                case END_ELEMENT:
                    switch (element) {
                        case FLOW:
                            return flow;
                        default:
                            throw BatchLogger.LOGGER.unexpectedXmlElement(element.getLocalName(), reader.getLocation());
                    }
            }
        }
        throw BatchLogger.LOGGER.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
    }

    private static Split parseSplit(final XMLStreamReader reader) throws XMLStreamException {
        final Split split = new Split(getAttributeValue(reader, XmlAttribute.ID));
        split.setAttributeNext(getAttributeValue(reader, XmlAttribute.NEXT));
        while (reader.hasNext()) {
            final int eventType = reader.next();
            if (eventType != START_ELEMENT && eventType != END_ELEMENT) {
                continue;
            }
            final XmlElement element = XmlElement.forName(reader.getLocalName());
            switch (eventType) {
                case START_ELEMENT:
                    if (element == XmlElement.FLOW) {
                        split.addFlow(parseFlow(reader));
                    } else {
                        throw BatchLogger.LOGGER.unexpectedXmlElement(element.getLocalName(), reader.getLocation());
                    }
                    break;
                case END_ELEMENT:
                    if (element == XmlElement.SPLIT) {
                        return split;
                    } else {
                        throw BatchLogger.LOGGER.unexpectedXmlElement(element.getLocalName(), reader.getLocation());
                    }
            }
        }
        throw BatchLogger.LOGGER.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
    }

    private static Properties parseProperties(final XMLStreamReader reader) throws XMLStreamException {
        final Properties properties = new Properties();
        properties.setPartition(getAttributeValue(reader, XmlAttribute.PARTITION));
        while (reader.hasNext()) {
            final int eventType = reader.next();
            if (eventType != START_ELEMENT && eventType != END_ELEMENT) {
                continue;
            }
            final XmlElement element = XmlElement.forName(reader.getLocalName());
            switch (eventType) {
                case START_ELEMENT:
                    if (element == XmlElement.PROPERTY) {
                        properties.add(getAttributeValue(reader, XmlAttribute.NAME), getAttributeValue(reader, XmlAttribute.VALUE));
                    } else {
                        throw BatchLogger.LOGGER.unexpectedXmlElement(element.getLocalName(), reader.getLocation());
                    }
                    break;
                case END_ELEMENT:
                    if (element == XmlElement.PROPERTIES) {
                        return properties;
                    } else if (element != XmlElement.PROPERTY) {
                        throw BatchLogger.LOGGER.unexpectedXmlElement(element.getLocalName(), reader.getLocation());
                    }
            }
        }
        throw BatchLogger.LOGGER.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
    }

    private static List<RefArtifact> parseListeners(final XMLStreamReader reader) throws XMLStreamException {
        final List<RefArtifact> listeners = new ArrayList<RefArtifact>();
        while (reader.hasNext()) {
            final int eventType = reader.next();
            if (eventType != START_ELEMENT && eventType != END_ELEMENT) {
                continue;
            }
            final XmlElement element = XmlElement.forName(reader.getLocalName());
            switch (eventType) {
                case START_ELEMENT:
                    if (element == XmlElement.LISTENER) {
                        listeners.add(parseRefArtifact(reader, element));
                    } else {
                        throw BatchLogger.LOGGER.unexpectedXmlElement(element.getLocalName(), reader.getLocation());
                    }
                    break;
                case END_ELEMENT:
                    if (element == XmlElement.LISTENERS) {
                        return listeners;
                    } else {
                        throw BatchLogger.LOGGER.unexpectedXmlElement(element.getLocalName(), reader.getLocation());
                    }
            }
        }
        throw BatchLogger.LOGGER.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
    }

    private static RefArtifact parseRefArtifact(final XMLStreamReader reader, final XmlElement artifactElementType) throws XMLStreamException {
        final RefArtifact refArtifact = new RefArtifact(getAttributeValue(reader, XmlAttribute.REF));
        while (reader.hasNext()) {
            final int eventType = reader.next();
            if (eventType != START_ELEMENT && eventType != END_ELEMENT) {
                continue;
            }
            final XmlElement element = XmlElement.forName(reader.getLocalName());
            switch (eventType) {
                case START_ELEMENT:
                    if (element == XmlElement.PROPERTIES) {
                        refArtifact.setProperties(parseProperties(reader));
                    } else {
                        throw BatchLogger.LOGGER.unexpectedXmlElement(element.getLocalName(), reader.getLocation());
                    }
                    break;
                case END_ELEMENT:
                    if (element == artifactElementType) {
                        return refArtifact;
                    } else {
                        throw BatchLogger.LOGGER.unexpectedXmlElement(element.getLocalName(), reader.getLocation());
                    }
            }
        }
        throw BatchLogger.LOGGER.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
    }

    private static Chunk parseChunk(final XMLStreamReader reader) throws XMLStreamException {
        final Chunk chunk = new Chunk();
        chunk.setCheckpointPolicy(getAttributeValue(reader, XmlAttribute.CHECKPOINT_POLICY));
        chunk.setItemCount(getAttributeValue(reader, XmlAttribute.ITEM_COUNT));
        chunk.setTimeLimit(getAttributeValue(reader, XmlAttribute.TIME_LIMIT));
        chunk.setSkipLimit(getAttributeValue(reader, XmlAttribute.SKIP_LIMIT));
        chunk.setRetryLimit(getAttributeValue(reader, XmlAttribute.RETRY_LIMIT));
        while (reader.hasNext()) {
            final int eventType = reader.next();
            if (eventType != START_ELEMENT && eventType != END_ELEMENT) {
                continue;
            }
            final XmlElement element = XmlElement.forName(reader.getLocalName());
            switch (eventType) {
                case START_ELEMENT:
                    switch (element) {
                        case READER:
                            chunk.setReader(parseRefArtifact(reader, XmlElement.READER));
                            break;
                        case PROCESSOR:
                            chunk.setProcessor(parseRefArtifact(reader, XmlElement.PROCESSOR));
                            break;
                        case WRITER:
                            chunk.setWriter(parseRefArtifact(reader, XmlElement.WRITER));
                            break;
                        case CHECKPOINT_ALGORITHM:
                            chunk.setCheckpointAlgorithm(parseRefArtifact(reader, XmlElement.CHECKPOINT_ALGORITHM));
                            break;
                        case SKIPPABLE_EXCEPTION_CLASSES:
                            chunk.setSkippableExceptionClasses(parseExceptionClassFilter(reader, XmlElement.SKIPPABLE_EXCEPTION_CLASSES));
                            break;
                        case RETRYABLE_EXCEPTION_CLASSES:
                            chunk.setRetryableExceptionClasses(parseExceptionClassFilter(reader, XmlElement.RETRYABLE_EXCEPTION_CLASSES));
                            break;
                        case NO_ROLLBACK_EXCEPTION_CLASSES:
                            chunk.setNoRollbackExceptionClasses(parseExceptionClassFilter(reader, XmlElement.NO_ROLLBACK_EXCEPTION_CLASSES));
                            break;
                        default:
                            throw BatchLogger.LOGGER.unexpectedXmlElement(element.getLocalName(), reader.getLocation());
                    }
                    break;
                case END_ELEMENT:
                    if (element == XmlElement.CHUNK) {
                        return chunk;
                    } else {
                        throw BatchLogger.LOGGER.unexpectedXmlElement(element.getLocalName(), reader.getLocation());
                    }
            }
        }
        throw BatchLogger.LOGGER.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
    }

    private static Partition parsePartition(final XMLStreamReader reader) throws XMLStreamException {
        final Partition partition = new Partition();
        while (reader.hasNext()) {
            final int eventType = reader.next();
            if (eventType != START_ELEMENT && eventType != END_ELEMENT) {
                continue;
            }
            final XmlElement element = XmlElement.forName(reader.getLocalName());
            switch (eventType) {
                case START_ELEMENT:
                    switch (element) {
                        case MAPPER:
                            partition.setMapper(parseRefArtifact(reader, XmlElement.MAPPER));
                            break;
                        case PLAN:
                            partition.setPlan(parsePartitionPlan(reader));
                            break;
                        case COLLECTOR:
                            partition.setCollector(parseRefArtifact(reader, XmlElement.COLLECTOR));
                            break;
                        case ANALYZER:
                            partition.setAnalyzer(parseRefArtifact(reader, XmlElement.ANALYZER));
                            break;
                        case REDUCER:
                            partition.setReducer(parseRefArtifact(reader, XmlElement.REDUCER));
                            break;
                        default:
                            throw BatchLogger.LOGGER.unexpectedXmlElement(element.getLocalName(), reader.getLocation());
                    }
                    break;
                case END_ELEMENT:
                    if (element == XmlElement.PARTITION) {
                        return partition;
                    } else {
                        throw BatchLogger.LOGGER.unexpectedXmlElement(element.getLocalName(), reader.getLocation());
                    }
            }
        }
        throw BatchLogger.LOGGER.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
    }

    private static PartitionPlan parsePartitionPlan(final XMLStreamReader reader) throws XMLStreamException {
        final PartitionPlan partitionPlan = new PartitionPlan();
        partitionPlan.setThreads(getAttributeValue(reader, XmlAttribute.THREADS));
        partitionPlan.setPartitions(getAttributeValue(reader, XmlAttribute.PARTITIONS));

        while (reader.hasNext()) {
            final int eventType = reader.next();
            if (eventType != START_ELEMENT && eventType != END_ELEMENT) {
                continue;
            }
            final XmlElement element = XmlElement.forName(reader.getLocalName());
            switch (eventType) {
                case START_ELEMENT:
                    if (element == XmlElement.PROPERTIES) {
                        partitionPlan.addProperties(parseProperties(reader));
                    } else {
                        throw BatchLogger.LOGGER.unexpectedXmlElement(element.getLocalName(), reader.getLocation());
                    }
                    break;
                case END_ELEMENT:
                    if (element == XmlElement.PLAN) {
                        return partitionPlan;
                    } else {
                        throw BatchLogger.LOGGER.unexpectedXmlElement(element.getLocalName(), reader.getLocation());
                    }
            }
        }
        throw BatchLogger.LOGGER.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
    }

    private static Transition.Next parseNext(final XMLStreamReader reader) throws XMLStreamException {
        final Transition.Next next = new Transition.Next(getAttributeValue(reader, XmlAttribute.ON));
        next.setTo(getAttributeValue(reader, XmlAttribute.TO));
        finishTransitionElement(reader, XmlElement.NEXT);
        return next;
    }

    private static Transition.Fail parseFail(final XMLStreamReader reader) throws XMLStreamException {
        final Transition.Fail fail = new Transition.Fail(getAttributeValue(reader, XmlAttribute.ON));
        fail.setExitStatus(getAttributeValue(reader, XmlAttribute.EXIT_STATUS));
        finishTransitionElement(reader, XmlElement.FAIL);
        return fail;
    }

    private static Transition.End parseEnd(final XMLStreamReader reader) throws XMLStreamException {
        final Transition.End end = new Transition.End(getAttributeValue(reader, XmlAttribute.ON));
        end.setExitStatus(getAttributeValue(reader, XmlAttribute.EXIT_STATUS));
        finishTransitionElement(reader, XmlElement.END);
        return end;
    }

    private static Transition.Stop parseStop(final XMLStreamReader reader) throws XMLStreamException {
        final Transition.Stop stop = new Transition.Stop(getAttributeValue(reader, XmlAttribute.ON), getAttributeValue(reader, XmlAttribute.RESTART));
        stop.setExitStatus(getAttributeValue(reader, XmlAttribute.EXIT_STATUS));
        finishTransitionElement(reader, XmlElement.STOP);
        return stop;
    }

    private static void finishTransitionElement(final XMLStreamReader reader, final XmlElement transitionElement) throws XMLStreamException {
        while (reader.hasNext()) {
            final int eventType = reader.next();
            if (eventType != START_ELEMENT && eventType != END_ELEMENT) {
                continue;
            }
            final XmlElement element = XmlElement.forName(reader.getLocalName());
            if (eventType == END_ELEMENT && element == transitionElement) {
                return;
            }
            throw BatchLogger.LOGGER.unexpectedXmlElement(element.getLocalName(), reader.getLocation());
        }
        throw BatchLogger.LOGGER.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
    }

    private static ExceptionClassFilter parseExceptionClassFilter(final XMLStreamReader reader, final XmlElement artifactElementType) throws XMLStreamException {
        final ExceptionClassFilter filter = new ExceptionClassFilter();
        while (reader.hasNext()) {
            final int eventType = reader.next();
            if (eventType != START_ELEMENT && eventType != END_ELEMENT) {
                continue;
            }
            final XmlElement element = XmlElement.forName(reader.getLocalName());
            switch (eventType) {
                case START_ELEMENT:
                    switch (element) {
                        case INCLUDE:
                            filter.addInclude(getAttributeValue(reader, XmlAttribute.CLASS));
                            break;
                        case EXCLUDE:
                            filter.addExclude(getAttributeValue(reader, XmlAttribute.CLASS));
                            break;
                        default:
                            throw BatchLogger.LOGGER.unexpectedXmlElement(element.getLocalName(), reader.getLocation());
                    }
                    break;
                case END_ELEMENT:
                    if (element == artifactElementType) {
                        return filter;
                    } else if (element != XmlElement.INCLUDE && element != XmlElement.EXCLUDE) {
                        throw BatchLogger.LOGGER.unexpectedXmlElement(element.getLocalName(), reader.getLocation());
                    }
            }
        }
        throw BatchLogger.LOGGER.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
    }
}
