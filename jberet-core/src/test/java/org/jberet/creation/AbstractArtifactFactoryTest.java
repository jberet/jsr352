package org.jberet.creation;

import org.junit.Before;
import org.junit.Test;

public class AbstractArtifactFactoryTest {

    private AbstractArtifactFactory factory = new AbstractArtifactFactory() {
        @Override
        public Object create(String ref, Class<?> cls, ClassLoader classLoader) throws Exception {
            return null;
        }

        @Override
        public Class<?> getArtifactClass(String ref, ClassLoader classLoader) {
            return null;
        }
    };

    private Class noPackageBatchletClass;
    private Object noPackageBatchlet;

    @Before
    public void setup() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        noPackageBatchletClass = Class.forName("NoPackageBatchlet");
        noPackageBatchlet = noPackageBatchletClass.newInstance();
    }

    @Test
    public void testDestroyShouldNotFailForDefaultPackage()  {
        factory.destroy(noPackageBatchlet);
    }

    @Test
    public void testDoInjectionShouldNotFailForDefaultPackage() throws Exception {
        factory.doInjection(noPackageBatchlet, noPackageBatchletClass, null, null, null, null);
    }
}
