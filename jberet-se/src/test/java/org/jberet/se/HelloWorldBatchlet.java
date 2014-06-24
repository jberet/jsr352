package org.jberet.se;

import javax.batch.api.Batchlet;
import javax.inject.Named;

@Named
public class HelloWorldBatchlet implements Batchlet {

	@Override
	public String process() throws Exception {
		return null;
	}

	@Override
	public void stop() throws Exception {
	}
}
