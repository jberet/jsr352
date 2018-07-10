/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import java.util.Properties
import javax.batch.runtime.context.{StepContext, JobContext}

val jobContext1 = jobContext.asInstanceOf[JobContext]
val stepContext1 = stepContext.asInstanceOf[StepContext]
val batchProperties1 = batchProperties.asInstanceOf[Properties]

println("jobName: " + jobContext1.getJobName())
println("stepName: " + stepContext1.getStepName())
val testName: String = batchProperties1.get("testName").asInstanceOf[String]
jobContext1.setExitStatus(testName)

return testName;
