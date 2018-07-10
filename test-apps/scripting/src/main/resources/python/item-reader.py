# Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0

rows = []
position = 0

def openBatchReader(checkpoint):
    global rows
    resource = batchProperties.get("resource")

    f = open(resource, 'rb')
    try:
        for row in f.readlines():
            columnValues = row.split(",")
            rows.append(columnValues)
    finally:
        f.close()

    if (checkpoint is None):
        position = checkpoint


def checkpointInfo():
    return position


def readItem():
    global position
    if (position >= len(rows)):
        return None

    item = rows[position]
    position += 1;
    return item;
