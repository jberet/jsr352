package org.jberet.testapps.deserialization;

import java.io.Serializable;
import java.util.List;
import javax.batch.api.chunk.AbstractItemWriter;
import javax.inject.Named;

@Named
public class ItemWriter1 extends AbstractItemWriter {
    @Override
    public void writeItems(final List<Object> items) throws Exception {
        System.out.printf("Writing %s%n", items);
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return new CheckpointInfo1(1);
    }

    @Override
    public void open(final Serializable checkpoint) throws Exception {
        super.open(checkpoint);
        System.out.printf("Writer open with checkpoint %s%n", checkpoint);
    }
}
