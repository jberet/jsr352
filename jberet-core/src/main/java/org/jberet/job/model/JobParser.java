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
import java.util.List;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jberet._private.BatchMessages;

import static javax.xml.stream.XMLStreamConstants.CDATA;
import static javax.xml.stream.XMLStreamConstants.CHARACTERS;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

/**
 * StAX parser for batch job xml and batch xml files.
 */
public final class JobParser {
    private static final String namespaceURI = null;

    /**
     * Parses a job xml input stream, which defines a batch job.
     *
     * @param inputStream the input source of the job xml definition
     * @param classLoader the current application class loader
     * @param xmlResolver the {@code javax.xml.stream.XMLResolver} for the job xml,
     *                    typically obtained from {@code org.jberet.spi.BatchEnvironment#getJobXmlResolver()}
     * @return the {@code Job} from parsing the input source
     * @throws XMLStreamException if failed to parse the input source
     *
     * @see org.jberet.spi.BatchEnvironment#getJobXmlResolver()
     */
    public static Job parseJob(final InputStream inputStream, final ClassLoader classLoader, final XMLResolver xmlResolver) throws XMLStreamException {
        final XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setXMLResolver(xmlResolver);
        final XMLStreamReader reader = factory.createXMLStreamReader(inputStream);
        
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
                                job = new Job(getAttributeValue(reader, XmlAttribute.ID, true));
                                job.setRestartable(getAttributeValue(reader, XmlAttribute.RESTARTABLE, false));
                                job.setAbstract(getAttributeValue(reader, XmlAttribute.ABSTRACT, false));

                                final String parentVal = getAttributeValue(reader, XmlAttribute.PARENT, false);
                                if (parentVal != null) {
                                    job.setParentAndJslName(parentVal, null);
                                    job.inheritingJobElements.add(job);
                                }
                                break;
                            case STEP:
                                job.addJobElement(parseStep(reader, job));
                                break;
                            case FLOW:
                                job.addJobElement(parseFlow(reader, job));
                                break;
                            case SPLIT:
                                job.addJobElement(parseSplit(reader, job));
                                break;
                            case DECISION:
                                job.addJobElement(parseDecision(reader));
                                break;
                            case PROPERTIES:
                                job.setProperties(parseProperties(reader));
                                break;
                            case LISTENERS:
                                job.setListeners(parseListeners(reader));
                                break;
                            default:
                                throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
                        }
                        break;
                    case END_ELEMENT:
                        if (element != XmlElement.JOB) {
                            throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
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
     * @return {@code BatchArtifacts} object from parsing the input source
     * @throws XMLStreamException if failed to parse the input source
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
                                batchArtifacts.addRef(getAttributeValue(reader, XmlAttribute.ID, true), getAttributeValue(reader, XmlAttribute.CLASS, true));
                                break;
                            default:
                                throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
                        }
                        break;
                    case END_ELEMENT:
                        if (element != XmlElement.BATCH_ARTIFACTS && element != XmlElement.REF) {
                            throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
                        }
                }
            }
        } finally {
            reader.close();
        }
        return batchArtifacts;
    }

    private static Step parseStep(final XMLStreamReader reader, final Job job) throws XMLStreamException {
        final Step step = new Step(getAttributeValue(reader, XmlAttribute.ID, true));
        step.setStartLimit(getAttributeValue(reader, XmlAttribute.START_LIMIT, false));
        step.setAllowStartIfComplete(getAttributeValue(reader, XmlAttribute.ALLOW_START_IF_COMPLETE, false));
        step.setAttributeNext(getAttributeValue(reader, XmlAttribute.NEXT, false));
        step.setAbstract(getAttributeValue(reader, XmlAttribute.ABSTRACT, false));

        final String parentVal = getAttributeValue(reader, XmlAttribute.PARENT, false);
        if (parentVal != null) {
            step.setParentAndJslName(parentVal, getAttributeValue(reader, XmlAttribute.JSL_NAME, false));
            job.inheritingJobElements.add(step);
        }

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
                            step.setListeners(parseListeners(reader));
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
                            if (step.getAttributeNext() != null) {
                                throw BatchMessages.MESSAGES.cannotHaveBothNextAttributeAndElement(reader.getLocation(), step.getAttributeNext());
                            }
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
                            throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
                    }
                    break;
                case END_ELEMENT:
                    switch (element) {
                        case STEP:
                            return step;
                        default:
                            throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
                    }
            }
        }
        throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
    }

    private static String getAttributeValue(final XMLStreamReader reader, final XmlAttribute attribute, final boolean required) {
        final String val = reader.getAttributeValue(namespaceURI, attribute.getLocalName());
        if (val == null && required) {
            throw BatchMessages.MESSAGES.failToGetAttribute(attribute.getLocalName(), reader.getLocation());
        }
        return val;
    }

    private static Decision parseDecision(final XMLStreamReader reader) throws XMLStreamException {
        final Decision decision = new Decision(getAttributeValue(reader, XmlAttribute.ID, true), getAttributeValue(reader, XmlAttribute.REF, true));
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
                            throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
                    }
                    break;
                case END_ELEMENT:
                    switch (element) {
                        case DECISION:
                            return decision;
                        default:
                            throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
                    }
            }
        }
        throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
    }

    private static Flow parseFlow(final XMLStreamReader reader, final Job job) throws XMLStreamException {
        final Flow flow = new Flow(getAttributeValue(reader, XmlAttribute.ID, true));
        flow.next = getAttributeValue(reader, XmlAttribute.NEXT, false);
        flow.setAbstract(getAttributeValue(reader, XmlAttribute.ABSTRACT, false));

        final String parentVal = getAttributeValue(reader, XmlAttribute.PARENT, false);
        if (parentVal != null) {
            flow.setParentAndJslName(parentVal, getAttributeValue(reader, XmlAttribute.JSL_NAME, false));
            job.inheritingJobElements.add(flow);
        }

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
                            flow.jobElements.add(parseDecision(reader));
                            break;
                        case FLOW:
                            flow.jobElements.add(parseFlow(reader, job));
                            break;
                        case SPLIT:
                            flow.jobElements.add(parseSplit(reader, job));
                            break;
                        case STEP:
                            flow.jobElements.add(parseStep(reader, job));
                            break;
                        case NEXT:
                            if (flow.getAttributeNext() != null) {
                                throw BatchMessages.MESSAGES.cannotHaveBothNextAttributeAndElement(reader.getLocation(), flow.getAttributeNext());
                            }
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
                            throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
                    }
                    break;
                case END_ELEMENT:
                    switch (element) {
                        case FLOW:
                            return flow;
                        default:
                            throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
                    }
            }
        }
        throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
    }

    private static Split parseSplit(final XMLStreamReader reader, final Job job) throws XMLStreamException {
        final Split split = new Split(getAttributeValue(reader, XmlAttribute.ID, true));
        split.setAttributeNext(getAttributeValue(reader, XmlAttribute.NEXT, false));
        while (reader.hasNext()) {
            final int eventType = reader.next();
            if (eventType != START_ELEMENT && eventType != END_ELEMENT) {
                continue;
            }
            final XmlElement element = XmlElement.forName(reader.getLocalName());
            switch (eventType) {
                case START_ELEMENT:
                    if (element == XmlElement.FLOW) {
                        split.addFlow(parseFlow(reader, job));
                    } else {
                        throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
                    }
                    break;
                case END_ELEMENT:
                    if (element == XmlElement.SPLIT) {
                        return split;
                    } else {
                        throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
                    }
            }
        }
        throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
    }

    private static Properties parseProperties(final XMLStreamReader reader) throws XMLStreamException {
        final Properties properties = new Properties();
        properties.setPartition(getAttributeValue(reader, XmlAttribute.PARTITION, false));
        properties.setMerge(getAttributeValue(reader, XmlAttribute.MERGE, false));

        while (reader.hasNext()) {
            final int eventType = reader.next();
            if (eventType != START_ELEMENT && eventType != END_ELEMENT) {
                continue;
            }
            final XmlElement element = XmlElement.forName(reader.getLocalName());
            switch (eventType) {
                case START_ELEMENT:
                    if (element == XmlElement.PROPERTY) {
                        properties.add(getAttributeValue(reader, XmlAttribute.NAME, true), getAttributeValue(reader, XmlAttribute.VALUE, false));
                    } else {
                        throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
                    }
                    break;
                case END_ELEMENT:
                    if (element == XmlElement.PROPERTIES) {
                        return properties;
                    } else if (element != XmlElement.PROPERTY) {
                        throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
                    }
            }
        }
        throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
    }

    private static Listeners parseListeners(final XMLStreamReader reader) throws XMLStreamException {
        final Listeners listeners = new Listeners();
        final List<RefArtifact> listenerList = listeners.getListeners();
        listeners.setMerge(getAttributeValue(reader, XmlAttribute.MERGE, false));

        while (reader.hasNext()) {
            final int eventType = reader.next();
            if (eventType != START_ELEMENT && eventType != END_ELEMENT) {
                continue;
            }
            final XmlElement element = XmlElement.forName(reader.getLocalName());
            switch (eventType) {
                case START_ELEMENT:
                    if (element == XmlElement.LISTENER) {
                        listenerList.add(parseRefArtifact(reader, element));
                    } else {
                        throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
                    }
                    break;
                case END_ELEMENT:
                    if (element == XmlElement.LISTENERS) {
                        return listeners;
                    } else {
                        throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
                    }
            }
        }
        throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
    }

    private static RefArtifact parseRefArtifact(final XMLStreamReader reader, final XmlElement artifactElementType) throws XMLStreamException {
        final RefArtifact refArtifact = new RefArtifact(getAttributeValue(reader, XmlAttribute.REF, false));
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
                    } else if(element == XmlElement.SCRIPT) {
                        refArtifact.setScript(parseScript(reader));
                    } else {
                        throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
                    }
                    break;
                case END_ELEMENT:
                    if (element == artifactElementType) {
                        return refArtifact;
                    } else {
                        throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
                    }
            }
        }
        throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
    }

    private static Script parseScript(final XMLStreamReader reader) throws XMLStreamException {
        final Script script = new Script(getAttributeValue(reader, XmlAttribute.TYPE, false),
                getAttributeValue(reader, XmlAttribute.SRC, false));
        StringBuilder charactersBuilder = new StringBuilder();
        StringBuilder scriptBuilder = new StringBuilder();
        while (reader.hasNext()) {
            final int eventType = reader.next();
            switch (eventType) {
                case CHARACTERS:
                	charactersBuilder.append(reader.getText());
                    break;
                case CDATA:
                	scriptBuilder.append(reader.getText());
                    break;
                case END_ELEMENT:
                    if (XmlElement.forName(reader.getLocalName()) == XmlElement.SCRIPT) {
                    	if (scriptBuilder.length() == 0) {
                    		String characters = charactersBuilder.toString().trim();
                            if(!characters.isEmpty()) {
                                script.content = characters;
                            }
                        } else {
                        	script.content = scriptBuilder.toString();
                        }
                        return script;
                    } else {
                        throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
                    }
            }
        }
        throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
    }



    private static Chunk parseChunk(final XMLStreamReader reader) throws XMLStreamException {
        final Chunk chunk = new Chunk();
        chunk.setCheckpointPolicy(getAttributeValue(reader, XmlAttribute.CHECKPOINT_POLICY, false));
        chunk.setItemCount(getAttributeValue(reader, XmlAttribute.ITEM_COUNT, false));
        chunk.setTimeLimit(getAttributeValue(reader, XmlAttribute.TIME_LIMIT, false));
        chunk.setSkipLimit(getAttributeValue(reader, XmlAttribute.SKIP_LIMIT, false));
        chunk.setRetryLimit(getAttributeValue(reader, XmlAttribute.RETRY_LIMIT, false));
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
                            throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
                    }
                    break;
                case END_ELEMENT:
                    if (element == XmlElement.CHUNK) {
                        return chunk;
                    } else {
                        throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
                    }
            }
        }
        throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
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
                            throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
                    }
                    break;
                case END_ELEMENT:
                    if (element == XmlElement.PARTITION) {
                        return partition;
                    } else {
                        throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
                    }
            }
        }
        throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
    }

    private static PartitionPlan parsePartitionPlan(final XMLStreamReader reader) throws XMLStreamException {
        final PartitionPlan partitionPlan = new PartitionPlan();
        partitionPlan.setThreads(getAttributeValue(reader, XmlAttribute.THREADS, false));
        partitionPlan.setPartitions(getAttributeValue(reader, XmlAttribute.PARTITIONS, false));

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
                        throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
                    }
                    break;
                case END_ELEMENT:
                    if (element == XmlElement.PLAN) {
                        return partitionPlan;
                    } else {
                        throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
                    }
            }
        }
        throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
    }

    private static Transition.Next parseNext(final XMLStreamReader reader) throws XMLStreamException {
        final Transition.Next next = new Transition.Next(getAttributeValue(reader, XmlAttribute.ON, true));
        next.setTo(getAttributeValue(reader, XmlAttribute.TO, true));
        finishTransitionElement(reader, XmlElement.NEXT);
        return next;
    }

    private static Transition.Fail parseFail(final XMLStreamReader reader) throws XMLStreamException {
        final Transition.Fail fail = new Transition.Fail(getAttributeValue(reader, XmlAttribute.ON, true));
        fail.setExitStatus(getAttributeValue(reader, XmlAttribute.EXIT_STATUS, false));
        finishTransitionElement(reader, XmlElement.FAIL);
        return fail;
    }

    private static Transition.End parseEnd(final XMLStreamReader reader) throws XMLStreamException {
        final Transition.End end = new Transition.End(getAttributeValue(reader, XmlAttribute.ON, true));
        end.setExitStatus(getAttributeValue(reader, XmlAttribute.EXIT_STATUS, false));
        finishTransitionElement(reader, XmlElement.END);
        return end;
    }

    private static Transition.Stop parseStop(final XMLStreamReader reader) throws XMLStreamException {
        final Transition.Stop stop = new Transition.Stop(getAttributeValue(reader, XmlAttribute.ON, true), getAttributeValue(reader, XmlAttribute.RESTART, false));
        stop.setExitStatus(getAttributeValue(reader, XmlAttribute.EXIT_STATUS, false));
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
            throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
        }
        throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
    }

    private static ExceptionClassFilter parseExceptionClassFilter(final XMLStreamReader reader, final XmlElement artifactElementType) throws XMLStreamException {
        final ExceptionClassFilter filter = new ExceptionClassFilter();
        filter.setMerge(getAttributeValue(reader, XmlAttribute.MERGE, false));

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
                            ExceptionClassFilter.addExceptionClassTo(getAttributeValue(reader, XmlAttribute.CLASS, true), filter.include);
                            break;
                        case EXCLUDE:
                            ExceptionClassFilter.addExceptionClassTo(getAttributeValue(reader, XmlAttribute.CLASS, true), filter.exclude);
                            break;
                        default:
                            throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
                    }
                    break;
                case END_ELEMENT:
                    if (element == artifactElementType) {
                        return filter;
                    } else if (element != XmlElement.INCLUDE && element != XmlElement.EXCLUDE) {
                        throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
                    }
            }
        }
        throw BatchMessages.MESSAGES.unexpectedXmlElement(reader.getLocalName(), reader.getLocation());
    }
}
