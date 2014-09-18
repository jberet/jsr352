=begin
# Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
# Cheng Fang - Initial API and implementation
=end

require 'csv'

$rows = []
$position = 0

def openBatchReader(checkpoint)
    resource = $batchProperties.get("resource")
    puts(resource)
    $rows = CSV.read(resource)

    if checkpoint != nil
        $position = checkpoint
    end

    puts($rows)
end

def closeBatchReader()
    puts('In reader close')
end


def checkpointInfo()
    return $position
end


def readItem()
    if $position >= $rows.length
        return nil
    end

    item = $rows[$position]
    $position += 1
    return item
end
