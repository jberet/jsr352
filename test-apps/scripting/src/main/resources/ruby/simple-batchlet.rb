=begin
 Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.

 All rights reserved. This program and the accompanying materials
 are made available under the terms of the Eclipse Public License v1.0
 which accompanies this distribution, and is available at
 http://www.eclipse.org/legal/epl-v10.html

 Contributors:
 Cheng Fang - Initial API and implementation
=end

def stop()
    puts 'In stop function'
end

# access built-in variables: jobContext, stepContext and batchProperties,
# set job exit status to the value of testName property, and
# return the value of testName property as step exit status,
#
def process()
    puts('jobName: ' + $jobContext.getJobName())
    puts('stepName: ' + $stepContext.getStepName())
    testName = $batchProperties.get('testName')
    $jobContext.setExitStatus(testName)
    return testName
end