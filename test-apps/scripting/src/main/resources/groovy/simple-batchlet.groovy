/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package groovy

def stop() {
    println('In stop function');
}

//access built-in variables: jobContext, stepContext and batchProperties,
//set job exit status to the value of testName property, and
//return the value of testName property as step exit status,
//
def process() {
    println('jobName: ' + jobContext.getJobName());
    println('stepName: ' + stepContext.getStepName());
    testName = batchProperties.get('testName');
    jobContext.setExitStatus(testName);
    return testName;
}
