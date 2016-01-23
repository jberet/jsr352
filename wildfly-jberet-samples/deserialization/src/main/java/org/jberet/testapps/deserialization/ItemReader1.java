package org.jberet.testapps.deserialization;

import java.io.Serializable;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.AbstractItemReader;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ItemReader1 extends AbstractItemReader {
    @Inject
    StepContext stepContext;

    @Inject
    @BatchProperty(name = "fail.on")
    int failOn;

    @Inject
    @BatchProperty(name = "number.limit")
    int numberLimit;

    int currentNumber;

    @Override
    public Object readItem() throws Exception {
        if (++currentNumber >= numberLimit) {
            return null;
        }
        stepContext.setPersistentUserData(new Data1("Current number: " + currentNumber));
        if (currentNumber == failOn) {
            throw new ArithmeticException("currentNumber matches fail.on number: " + currentNumber);
        }
        return currentNumber;
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return new CheckpointInfo1(currentNumber);
    }

    @Override
    public void open(final Serializable checkpoint) throws Exception {
        super.open(checkpoint);
        System.out.printf("Reader open with checkpoint %s%n", checkpoint);
        if(checkpoint!=null) {
            currentNumber = ((CheckpointInfo1) checkpoint).getNumber();
        }
    }
}
