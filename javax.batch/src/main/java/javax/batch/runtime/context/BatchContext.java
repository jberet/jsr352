package javax.batch.runtime.context;

/**
 * Base class for all batch context types.
 *
 */
public interface BatchContext <T> {
	/**
	 * The getId method returns the context id. This
	 * is value of the id attribute from the Job 
	 * XML execution element corresponding to this
	 * context  type.    
	 * @return id string
	 */
	public String getId();
	/**
	 * The getTransientUserData method returns a transient data object 
	 * belonging to the current Job XML execution element. 
	 * @return user-specified type
	 */
	public T getTransientUserData();
	/**
	 * The setTransientUserData method stores a transient data object into 
	 * the current batch context. 
	 * @param data is the user-specified type
	 */
	public void setTransientUserData(T data);

}
