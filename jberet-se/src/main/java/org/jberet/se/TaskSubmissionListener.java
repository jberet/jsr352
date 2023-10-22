package org.jberet.se;

/**
 * Invoked around a task submission
 * 
 * @author Formenti Lorenzo
 */
public interface TaskSubmissionListener {
	
	void beforeSubmit();
	
	void afterSubmit();

}
