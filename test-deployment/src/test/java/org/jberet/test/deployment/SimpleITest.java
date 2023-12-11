package org.jberet.test.deployment;

import io.restassured.RestAssured;
import org.junit.Assert;
import org.junit.Test;

public class SimpleITest {
    @Test
    public void testSimple() throws Exception {
        Assert.assertEquals("OK",
                RestAssured.given().baseUri("http://localhost/test-deployment").basePath("/simple").port(8080)
                        .get().asString());
    }

}
