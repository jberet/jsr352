=begin
# Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
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
