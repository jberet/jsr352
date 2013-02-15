package javax.batch.runtime.context;

import java.util.List;

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
	/**
	 * The getBatchContexts method returns a list of BatchContexts
	 * corresponding to a compound Job XML execution element, 
	 * either a split or a flow. The batch context of a compound
	 * execution element contains a list of batch contexts of 
	 * the execution elements contained within that compound
	 * execution element.  For example, if this batch context
	 * belongs to a split, the list of batch contexts is the
	 * flow contexts belonging to the flows in that split; if
	 * this batch context belongs to a flow, the list of batch 
	 * contexts may contain a combination of split and step
	 * batch contexts. For regular execution elements (e.g.
	 * job, step) this method returns null.
	 * @return list of BatchContexts
	 */
	public List<BatchContext<T>> getBatchContexts();
	
	/**
     * The getExitStatus method simply returns the exit status value stored into
     * the context through the setExitStatus method or null.
     * 
     * @return exit status string
     */
    public String getExitStatus();

    /**
     * The setExitStatus method assigns the user-specified exit status for the
     * current context.
     * 
     * @Param status string
     */
    public void setExitStatus(String status);
	

}
