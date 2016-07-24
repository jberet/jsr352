import javax.batch.api.AbstractBatchlet;

public class NoPackageBatchlet extends AbstractBatchlet {
    @Override
    public String process() throws Exception {
        return null;
    }
}
