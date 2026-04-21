/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2023 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jberet.test.deployment;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wildfly.testing.junit.extension.annotation.GenerateDeployment;
import org.wildfly.testing.junit.extension.annotation.RequestPath;
import org.wildfly.testing.junit.extension.annotation.ServerResource;
import org.wildfly.testing.junit.extension.annotation.WildFlyTest;

@WildFlyTest
public class SimpleITest {

    private static final StringAsset BEANS_XML = new StringAsset("""
            <?xml version="1.0" encoding="UTF-8"?>
            <!-- Marker file indicating CDI should be enabled -->
            <beans xmlns="https://jakarta.ee/xml/ns/jakartaee" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="
                     https://jakarta.ee/xml/ns/jakartaee
                     https://jakarta.ee/xml/ns/jakartaee/beans_4_0.xsd"
                   bean-discovery-mode="all">
            </beans>
            """);

    @GenerateDeployment()
    public static void createDeployment(final WebArchive war) {
        war.addClasses(SimpleApp.class, SimpleBatchlet.class, SimpleResource.class)
                .addAsWebInfResource("simple.xml", "classes/META-INF/batch-jobs/simple.xml")
                .addAsWebInfResource(BEANS_XML, "beans.xml");
    }

    @Test
    public void testSimple(@ServerResource @RequestPath("simple") final URI uri) throws Exception {
        final HttpClient client = HttpClient.newHttpClient();
        final HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .build();
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals("OK", response.body());
    }

}
