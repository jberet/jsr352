package javax.batch.api.partition;

import java.util.Properties;

/**
 * The PartitionPlanImpl class provides a basic implementation
 * of the PartitionPlan interface. 
 */
public class PartitionPlanImpl implements PartitionPlan {

	private int partitions= 0;
	private boolean override= false; 
	private int threads= 0;
	Properties[] partitionProperties= null;  
	
	@Override
	public void setPartitions(int count) {
		partitions= count;
		// default thread count to partition count 
		if (threads == 0) threads= count; 
	}

	@Override
	public void setThreads(int count) {
		threads= count; 		
	}

	@Override
	public void setPartitionsOverride(boolean override) {
		this.override= override; 
		
	}

	@Override
	public boolean getPartitionsOverride() {
		return override;
	}
	
	@Override
	public void setPartitionProperties(Properties[] props) {
		partitionProperties= props;		
	}

	@Override
	public int getPartitions() {
		return partitions;
	}

	@Override
	public int getThreads() {
		return threads;
	}

	@Override
	public Properties[] getPartitionProperties() {
		return partitionProperties; 
	}

}
