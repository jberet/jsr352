/*
 * Copyright (c) 2018 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.rest.commons.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.batch.operations.BatchRuntimeException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jberet.job.model.Decision;
import org.jberet.job.model.DecisionBuilder;
import org.jberet.job.model.Flow;
import org.jberet.job.model.FlowBuilder;
import org.jberet.job.model.Job;
import org.jberet.job.model.JobBuilder;
import org.jberet.job.model.Split;
import org.jberet.job.model.SplitBuilder;
import org.jberet.job.model.Step;
import org.jberet.job.model.StepBuilder;
import org.jberet.job.model.XmlAttribute;
import org.jberet.job.model.XmlElement;
import org.jberet.rest.commons._private.RestCommonsMessages;

/**
 * A mapper class between {@link org.jberet.job.model.Job} object and
 * its JSON representation.
 *
 * @since 1.3.0.Final
 */
public final class JsonJobMapper {
    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    private JsonJobMapper() {
    }

    /**
     * Converts the JSON string to {@link Job} object.
     *
     * @param json the JSON string representing the batch job definition
     * @return a {@link Job} converted from {@code json}
     *
     * @throws BatchRuntimeException if any errors when reading job definition
     */
    public static Job toJob(final String json) throws BatchRuntimeException {
        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode jobNode;
        try {
            jobNode = objectMapper.readTree(json).get(XmlElement.JOB.getLocalName());
        } catch (IOException e) {
            throw RestCommonsMessages.MESSAGES.failToReadJobDefinition(e);
        }

        final String jobId = getRequiredTextValue(jobNode, XmlAttribute.ID, XmlElement.JOB);
        JobBuilder jobBuilder = new JobBuilder(jobId);
        mapJob(jobNode, jobBuilder);
        return jobBuilder.build();
    }

    private static void mapJob(final JsonNode jobNode, final JobBuilder jobBuilder) {
        // job attributes
        applyBooleanValue(jobNode, XmlAttribute.RESTARTABLE, jobBuilder::restartable);

        // job elements
        jobBuilder.properties(parseProperties(jobNode, null));
        mapListeners(jobNode.get(XmlElement.LISTENERS.getLocalName()), jobBuilder::listener);
        mapFlows(jobNode.get(XmlElement.FLOW.getLocalName()), jobBuilder::flow);
        mapSplits(jobNode.get(XmlElement.SPLIT.getLocalName()), jobBuilder::split);
        mapSteps(jobNode.get(XmlElement.STEP.getLocalName()), jobBuilder::step);
        mapDecisions(jobNode.get(XmlElement.DECISION.getLocalName()), jobBuilder::decision);
    }

    private static void mapFlows(final JsonNode flowNode, final Consumer<Flow> applyMethod) {
        if (flowNode == null) {
            return;
        }
        if (flowNode.isArray()) {
            final Iterator<JsonNode> elements = flowNode.elements();
            while (elements.hasNext()) {
                mapSingleFlow(elements.next(), applyMethod);
            }
        } else {
            mapSingleFlow(flowNode, applyMethod);
        }
    }

    private static void mapSingleFlow(final JsonNode singleFlowNode, final Consumer<Flow> applyMethod) {
        final FlowBuilder flowBuilder =
                new FlowBuilder(getRequiredTextValue(singleFlowNode, XmlAttribute.ID, XmlElement.FLOW));
        applyTextValue(singleFlowNode, XmlAttribute.NEXT, flowBuilder::next);
        mapTransitions(singleFlowNode, null, flowBuilder, null);

        mapFlows(singleFlowNode.get(XmlElement.FLOW.getLocalName()), flowBuilder::flow);
        mapSplits(singleFlowNode.get(XmlElement.SPLIT.getLocalName()), flowBuilder::split);
        mapSteps(singleFlowNode.get(XmlElement.STEP.getLocalName()), flowBuilder::step);
        mapDecisions(singleFlowNode.get(XmlElement.DECISION.getLocalName()), flowBuilder::decision);
        applyMethod.accept(flowBuilder.build());
    }

    private static void mapSplits(final JsonNode splitNode, final Consumer<Split> applyMethod) {
        if (splitNode == null) {
            return;
        }
        if (splitNode.isArray()) {
            final Iterator<JsonNode> elements = splitNode.elements();
            while (elements.hasNext()) {
                mapSingleSplit(elements.next(), applyMethod);
            }
        } else {
            mapSingleSplit(splitNode, applyMethod);
        }
    }

    private static void mapSingleSplit(final JsonNode singleSplitNode, final Consumer<Split> applyMethod) {
        final SplitBuilder splitBuilder = new SplitBuilder(getRequiredTextValue(singleSplitNode, XmlAttribute.ID,
                XmlElement.SPLIT));
        applyTextValue(singleSplitNode, XmlAttribute.NEXT, splitBuilder::next);
        mapFlows(singleSplitNode.get(XmlElement.FLOW.getLocalName()), splitBuilder::flow);
        applyMethod.accept(splitBuilder.build());
    }

    private static void mapDecisions(final JsonNode decisionNode, final Consumer<Decision> applyMethod) {
        if (decisionNode == null) {
            return;
        }
        if (decisionNode.isArray()) {
            final Iterator<JsonNode> elements = decisionNode.elements();
            while (elements.hasNext()) {
                mapSingleDecision(elements.next(), applyMethod);
            }
        } else {
            mapSingleDecision(decisionNode, applyMethod);
        }
    }

    private static void mapSingleDecision(final JsonNode singleDecisionNode, final Consumer<Decision> applyMethod) {
        final DecisionBuilder decisionBuilder = new DecisionBuilder(
                getRequiredTextValue(singleDecisionNode, XmlAttribute.ID, XmlElement.JOB, XmlElement.DECISION),
                getRequiredTextValue(singleDecisionNode, XmlAttribute.REF, XmlElement.JOB, XmlElement.DECISION));
        decisionBuilder.properties(parseProperties(singleDecisionNode, null));
        mapTransitions(singleDecisionNode, null, null, decisionBuilder);
        applyMethod.accept(decisionBuilder.build());
    }

    private static void mapTransitions(final JsonNode containingNode,
                                       final StepBuilder stepBuilder,
                                       final FlowBuilder flowBuilder,
                                       final DecisionBuilder decisionBuilder) {
        mapTransitionSingleOrArray(containingNode, XmlElement.END,
                (endNodeSingle) -> mapSingleEnd(endNodeSingle, stepBuilder, flowBuilder, decisionBuilder));

        mapTransitionSingleOrArray(containingNode, XmlElement.FAIL,
                (failNodeSingle) -> mapSingleFail(failNodeSingle, stepBuilder, flowBuilder, decisionBuilder));

        mapTransitionSingleOrArray(containingNode, XmlElement.STOP,
                (stopNodeSingle) -> mapSingleStop(stopNodeSingle, stepBuilder, flowBuilder, decisionBuilder));

        mapTransitionSingleOrArray(containingNode, XmlElement.NEXT,
                (nextNodeSingle) -> mapSingleNext(nextNodeSingle, stepBuilder, flowBuilder, decisionBuilder));
    }

    private static void mapTransitionSingleOrArray(final JsonNode containingNode,
                                                   final XmlElement transitionElementName,
                                                   final Consumer<JsonNode> function) {
        final JsonNode elementNode = containingNode.get(transitionElementName.getLocalName());
        if (elementNode != null) {
            if (elementNode.isArray()) {
                final Iterator<JsonNode> elements = elementNode.elements();
                while (elements.hasNext()) {
                    function.accept(elements.next());
                }
            } else {
                function.accept(elementNode);
            }
        }
    }

    private static void mapSingleEnd(final JsonNode singleEndNode,
                                     final StepBuilder stepBuilder,
                                     final FlowBuilder flowBuilder,
                                     final DecisionBuilder decisionBuilder) {
        final String on = getRequiredTextValue(singleEndNode, XmlAttribute.ON, XmlElement.END);
        final String exitStatus = singleEndNode.path(XmlAttribute.EXIT_STATUS.getLocalName()).asText(null);
        final String[] newExitStatus = exitStatus == null ? EMPTY_STRING_ARRAY : new String[]{exitStatus};
        if (stepBuilder != null) {
            stepBuilder.endOn(on).exitStatus(newExitStatus);
        } else if (flowBuilder != null) {
            flowBuilder.endOn(on).exitStatus(newExitStatus);
        } else if (decisionBuilder != null) {
            decisionBuilder.endOn(on).exitStatus(newExitStatus);
        }
    }

    private static void mapSingleFail(final JsonNode singleFailNode,
                                      final StepBuilder stepBuilder,
                                      final FlowBuilder flowBuilder,
                                      final DecisionBuilder decisionBuilder) {
        final String on = getRequiredTextValue(singleFailNode, XmlAttribute.ON, XmlElement.FAIL);
        final String exitStatus = singleFailNode.path(XmlAttribute.EXIT_STATUS.getLocalName()).asText(null);
        final String[] newExitStatus = exitStatus == null ? EMPTY_STRING_ARRAY : new String[]{exitStatus};
        if (stepBuilder != null) {
            stepBuilder.failOn(on).exitStatus(newExitStatus);
        } else if (flowBuilder != null) {
            flowBuilder.failOn(on).exitStatus(newExitStatus);
        } else if (decisionBuilder != null) {
            decisionBuilder.failOn(on).exitStatus(newExitStatus);
        }
    }

    private static void mapSingleStop(final JsonNode singleStopNode,
                                      final StepBuilder stepBuilder,
                                      final FlowBuilder flowBuilder,
                                      final DecisionBuilder decisionBuilder) {
        final String on = getRequiredTextValue(singleStopNode, XmlAttribute.ON, XmlElement.STOP);
        final String restartFrom = singleStopNode.path(XmlAttribute.RESTART.getLocalName()).asText(null);
        final String exitStatus = singleStopNode.path(XmlAttribute.EXIT_STATUS.getLocalName()).asText(null);
        final String[] newExitStatus = exitStatus == null ? EMPTY_STRING_ARRAY : new String[]{exitStatus};
        if (stepBuilder != null) {
            stepBuilder.stopOn(on).restartFrom(restartFrom).exitStatus(newExitStatus);
        } else if (flowBuilder != null) {
            flowBuilder.stopOn(on).restartFrom(restartFrom).exitStatus(newExitStatus);
        } else if (decisionBuilder != null) {
            decisionBuilder.stopOn(on).restartFrom(restartFrom).exitStatus(newExitStatus);
        }
    }

    private static void mapSingleNext(final JsonNode singleNextNode,
                                      final StepBuilder stepBuilder,
                                      final FlowBuilder flowBuilder,
                                      final DecisionBuilder decisionBuilder) {
        if (singleNextNode.isObject()) {
            final String on = getRequiredTextValue(singleNextNode, XmlAttribute.ON, XmlElement.NEXT);
            final String to = getRequiredTextValue(singleNextNode, XmlAttribute.TO, XmlElement.NEXT);
            if (stepBuilder != null) {
                stepBuilder.nextOn(on).to(to);
            } else if (flowBuilder != null) {
                flowBuilder.nextOn(on).to(to);
            } else if (decisionBuilder != null) {
                decisionBuilder.nextOn(on).to(to);
            }
        }
    }

    private static void mapSingleStep(final JsonNode step1Node, final Consumer<Step> applyMethod) {
        final StepBuilder stepBuilder = new StepBuilder(
                getRequiredTextValue(step1Node, XmlAttribute.ID, XmlElement.JOB, XmlElement.STEP));

        applyIntValue(step1Node, XmlAttribute.START_LIMIT, stepBuilder::startLimit);
        applyBooleanValue(step1Node, XmlAttribute.ALLOW_START_IF_COMPLETE, stepBuilder::allowStartIfComplete);
        applyTextValue(step1Node, XmlAttribute.NEXT, stepBuilder::next);

        stepBuilder.properties(parseProperties(step1Node, null));
        applyRefArtifactWithProperties(step1Node.get(XmlElement.BATCHLET.getLocalName()), stepBuilder::batchlet,
                XmlElement.STEP, XmlElement.BATCHLET);

        mapListeners(step1Node.get(XmlElement.LISTENERS.getLocalName()), stepBuilder::listener);
        mapChunk(step1Node.get(XmlElement.CHUNK.getLocalName()), stepBuilder);
        mapPartition(step1Node.get(XmlElement.PARTITION.getLocalName()), stepBuilder);
        mapTransitions(step1Node, stepBuilder, null, null);

        applyMethod.accept(stepBuilder.build());
    }

    private static void mapSteps(final JsonNode stepNode, final Consumer<Step> applyMethod) {
        if (stepNode == null) {
            return;
        }
        if (stepNode.isArray()) {
            final Iterator<JsonNode> elements = stepNode.elements();
            while (elements.hasNext()) {
                mapSingleStep(elements.next(), applyMethod);
            }
        } else {
            mapSingleStep(stepNode, applyMethod);
        }
    }

    private static void mapChunk(final JsonNode chunkNode, final StepBuilder stepBuilder) {
        if (chunkNode == null) {
            return;
        }
        applyIntValue(chunkNode, XmlAttribute.RETRY_LIMIT, stepBuilder::retryLimit);
        applyIntValue(chunkNode, XmlAttribute.SKIP_LIMIT, stepBuilder::skipLimit);
        applyIntValue(chunkNode, XmlAttribute.TIME_LIMIT, stepBuilder::timeLimit);
        applyIntValue(chunkNode, XmlAttribute.ITEM_COUNT, stepBuilder::itemCount);
        applyTextValue(chunkNode, XmlAttribute.CHECKPOINT_POLICY, stepBuilder::checkpointPolicy);

        applyRefArtifactWithProperties(chunkNode.get(XmlElement.READER.getLocalName()),
                stepBuilder::reader, XmlElement.CHUNK, XmlElement.READER);

        applyRefArtifactWithProperties(chunkNode.get(XmlElement.PROCESSOR.getLocalName()),
                stepBuilder::processor, XmlElement.CHUNK, XmlElement.PROCESSOR);

        applyRefArtifactWithProperties(chunkNode.get(XmlElement.WRITER.getLocalName()),
                stepBuilder::writer, XmlElement.CHUNK, XmlElement.WRITER);

        applyRefArtifactWithProperties(chunkNode.get(XmlElement.CHECKPOINT_ALGORITHM.getLocalName()),
                stepBuilder::checkpointAlgorithm, XmlElement.CHUNK, XmlElement.CHECKPOINT_ALGORITHM);

        // map exception class filter
        mapExceptionFilter(chunkNode.get(XmlElement.SKIPPABLE_EXCEPTION_CLASSES.getLocalName()),
                stepBuilder::skippableExceptionsInclude, stepBuilder::skippableExceptionsExclude);
        mapExceptionFilter(chunkNode.get(XmlElement.RETRYABLE_EXCEPTION_CLASSES.getLocalName()),
                stepBuilder::retryableExceptionsInclude, stepBuilder::retryableExceptionsExclude);
        mapExceptionFilter(chunkNode.get(XmlElement.NO_ROLLBACK_EXCEPTION_CLASSES.getLocalName()),
                stepBuilder::noRollbackExceptionsInclude, stepBuilder::noRollbackExceptionsExclude);
    }

    private static void mapExceptionFilter(final JsonNode exceptionFilterNode,
                                           final Consumer<List<String>> includeMethod,
                                           final Consumer<List<String>> excludeMethod) {
        if (exceptionFilterNode != null) {
            final JsonNode includeNode = exceptionFilterNode.get(XmlElement.INCLUDE.getLocalName());
            if (includeNode != null) {
                final List<String> includedClassNames = includeNode.findValuesAsText(XmlAttribute.CLASS.getLocalName());
                includeMethod.accept(includedClassNames);
            }
            final JsonNode excludeNode = exceptionFilterNode.get(XmlElement.EXCLUDE.getLocalName());
            if (excludeNode != null) {
                final List<String> excludedClassNames = excludeNode.findValuesAsText(XmlAttribute.CLASS.getLocalName());
                excludeMethod.accept(excludedClassNames);
            }
        }
    }

    private static void mapPartition(final JsonNode partitionNode, final StepBuilder stepBuilder) {
        if (partitionNode != null) {
            applyRefArtifactWithProperties(partitionNode.get(XmlElement.COLLECTOR.getLocalName()),
                    stepBuilder::partitionCollector, XmlElement.PARTITION, XmlElement.COLLECTOR);

            applyRefArtifactWithProperties(partitionNode.get(XmlElement.REDUCER.getLocalName()),
                    stepBuilder::partitionReducer, XmlElement.PARTITION, XmlElement.REDUCER);

            applyRefArtifactWithProperties(partitionNode.get(XmlElement.ANALYZER.getLocalName()),
                    stepBuilder::partitionAnalyzer, XmlElement.PARTITION, XmlElement.ANALYZER);

            applyRefArtifactWithProperties(partitionNode.get(XmlElement.MAPPER.getLocalName()),
                    stepBuilder::partitionMapper, XmlElement.PARTITION, XmlElement.MAPPER);

            final JsonNode planNode = partitionNode.get(XmlElement.PLAN.getLocalName());
            if (planNode != null) {
                int partitions = 0;
                int threads = 0;
                final JsonNode partitionsNode = planNode.get(XmlAttribute.PARTITIONS.getLocalName());
                if (partitionsNode != null) {
                    partitions = partitionsNode.asInt();
                }
                final JsonNode threadsNode = planNode.get(XmlAttribute.THREADS.getLocalName());
                if (threadsNode != null) {
                    threads = threadsNode.asInt();
                }
                List<Properties> partitionPropertiesList = new ArrayList<>();
                parseProperties(planNode, partitionPropertiesList);
                stepBuilder.partitionPlan(partitions, threads, partitionPropertiesList);
            }
        }
    }

    private static void mapListeners(final JsonNode listenersNode,
                                     final BiConsumer<String, Properties> applyMethod) {
        if (listenersNode != null) {
            final JsonNode listenerNode = listenersNode.get(XmlElement.LISTENER.getLocalName());
            if (listenerNode != null) {
                if (listenerNode.isArray()) {
                    final Iterator<JsonNode> elements = listenerNode.elements();
                    while (elements.hasNext()) {
                        applyRefArtifactWithProperties(elements.next(), applyMethod,
                                XmlElement.LISTENERS, XmlElement.LISTENER);
                    }
                } else {
                    applyRefArtifactWithProperties(listenerNode, applyMethod,
                            XmlElement.LISTENERS, XmlElement.LISTENER);
                }
            }
        }
    }

    private static void applyRefArtifactWithProperties(final JsonNode artifactNode,
                                                       final BiConsumer<String, Properties> applyMethod,
                                                       final XmlElement... parentElements) {
        if (artifactNode != null) {
            final String ref = getRequiredTextValue(artifactNode, XmlAttribute.REF, parentElements);
            final Properties properties = parseProperties(artifactNode, null);
            applyMethod.accept(ref, properties);
        }
    }

    private static void applyIntValue(final JsonNode parentNode,
                                      final XmlAttribute attr,
                                      final Consumer<Integer> applyMethod) {
        final JsonNode attrNode = parentNode.get(attr.getLocalName());
        if (attrNode != null) {
            final int intValue = attrNode.intValue();
            applyMethod.accept(intValue);
        }
    }

    private static void applyBooleanValue(final JsonNode parentNode,
                                          final XmlAttribute attr,
                                          final Consumer<Boolean> applyMethod) {
        final JsonNode attrNode = parentNode.get(attr.getLocalName());
        if (attrNode != null) {
            final boolean booleanValue = attrNode.booleanValue();
            applyMethod.accept(booleanValue);
        }
    }

    private static void applyTextValue(final JsonNode parentNode,
                                       final XmlAttribute attr,
                                       final Consumer<String> applyMethod) {
        final JsonNode attrNode = parentNode.get(attr.getLocalName());
        if (attrNode != null) {
            if (attr != XmlAttribute.NEXT || attrNode.isTextual()) {
                final String textValue = attrNode.textValue();
                applyMethod.accept(textValue);
            }
        }
    }

    private static java.util.Properties parseProperties(final JsonNode containingNode,
                                                        final List<Properties> propertiesList) {
        if (containingNode == null) {
            return null;
        }
        final JsonNode propertiesNode = containingNode.get(XmlElement.PROPERTIES.getLocalName());
        if (propertiesNode == null) {
            return null;
        }

        if (propertiesNode.isArray()) {
            //this is a list of properties under a partition plan
            final Iterator<JsonNode> elements = propertiesNode.elements();
            while (elements.hasNext()) {
                final JsonNode next = elements.next();
                final Properties properties = new Properties();
                parsePropertyEntries(next, properties);
                propertiesList.add(properties);
            }
            return null;
        } else {
            //this is a normal properties that contains one or more entries
            final Properties properties = new Properties();
            parsePropertyEntries(propertiesNode, properties);
            return properties;
        }
    }

    private static void parsePropertyEntries(final JsonNode containingNode, final Properties properties) {
        JsonNode propertyNode = containingNode.get(XmlElement.PROPERTY.getLocalName());
        if (propertyNode != null) {
            if (propertyNode.isArray()) {
                //there are multiple entries under this properties
                final Iterator<JsonNode> elements = propertyNode.elements();
                while (elements.hasNext()) {
                    final JsonNode next = elements.next();
                    properties.setProperty(getRequiredTextValue(next, XmlAttribute.NAME,
                            XmlElement.PROPERTIES, XmlElement.PROPERTY),
                            getRequiredTextValue(next, XmlAttribute.VALUE,
                                    XmlElement.PROPERTIES, XmlElement.PROPERTY));
                }
            } else {
                //there is only one entry under this properties
                properties.setProperty(getRequiredTextValue(propertyNode, XmlAttribute.NAME,
                        XmlElement.PROPERTIES, XmlElement.PROPERTY),
                        getRequiredTextValue(propertyNode, XmlAttribute.VALUE,
                                XmlElement.PROPERTIES, XmlElement.PROPERTY));
            }
        }
    }

    private static String getRequiredTextValue(final JsonNode node, final XmlAttribute attr,
                                               final XmlElement... parents) {
        final JsonNode node1 = node.get(attr.getLocalName());
        if (node1 == null) {
            final String[] parentsAsStrings = Arrays.stream(parents).map(XmlElement::getLocalName).toArray(String[]::new);
            throw RestCommonsMessages.MESSAGES.expectingJsonElement(attr.getLocalName(), parentsAsStrings);
        }
        return node1.textValue();
    }

}
