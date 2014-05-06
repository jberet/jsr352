/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
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