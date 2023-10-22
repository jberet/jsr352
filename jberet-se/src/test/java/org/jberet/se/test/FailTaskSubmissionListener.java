package org.jberet.se.test;

import org.jberet.se.TaskSubmissionListener;

public class FailTaskSubmissionListener implements TaskSubmissionListener {

	@Override
	public void beforeSubmit() {
		throw new RuntimeException();
	}

	@Override
	public void afterSubmit() {
		throw new RuntimeException();
	}

}
