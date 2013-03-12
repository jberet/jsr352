package javax.batch.operations;

public class JobExecutionAlreadyCompleteException extends
BatchOperationsRuntimeException {

	public JobExecutionAlreadyCompleteException(Throwable th,
			String localizedMessage) {
		super(th, localizedMessage);
		// TODO Auto-generated constructor stub
	}

	public JobExecutionAlreadyCompleteException(Throwable th) {
		super(th);
		// TODO Auto-generated constructor stub
	}
	
	public JobExecutionAlreadyCompleteException(String localizedMessage) {
		super(localizedMessage);
		// TODO Auto-generated constructor stub
	}
	
	public JobExecutionAlreadyCompleteException() {
		super();
		// TODO Auto-generated constructor stub
	}
}
