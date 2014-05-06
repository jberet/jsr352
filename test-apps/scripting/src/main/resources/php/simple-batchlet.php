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

<?php
    import java.util.HashMap;

    echo "jobName: " . $jobContext->getJobName() . PHP_EOL;
    echo "stepName: " . $stepContext->getStepName() . PHP_EOL;

    $testName = $batchProperties["testName"];
    $jobContext->setExitStatus($testName);
    return $testName;
?>