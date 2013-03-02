package javax.batch.operations.exception;

public class JobExecutionNotMostRecentException extends
BatchOperationsRuntimeException{

	public JobExecutionNotMostRecentException(Throwable th,
			String localizedMessage) {
		super(th, localizedMessage);
		// TODO Auto-generated constructor stub
	}
	
	public JobExecutionNotMostRecentException(String localizedMessage) {
		super(localizedMessage);
		// TODO Auto-generated constructor stub
	}
	
	public JobExecutionNotMostRecentException(Throwable th) {
		super(th);
		// TODO Auto-generated constructor stub
	}
	
	public JobExecutionNotMostRecentException() {
		super();
		// TODO Auto-generated constructor stub
	}

}
