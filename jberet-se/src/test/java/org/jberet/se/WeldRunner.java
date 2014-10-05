package org.jberet.se;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

public class WeldRunner extends BlockJUnit4ClassRunner {

	private final WeldContainer weldContainer;

	public WeldRunner(Class<?> klass) throws InitializationError {
		super(klass);
		this.weldContainer = new Weld().initialize();
	}

	@Override
	protected Object createTest() throws Exception {
		return this.weldContainer.instance()
				.select(super.getTestClass().getJavaClass()).get();
	}
}
