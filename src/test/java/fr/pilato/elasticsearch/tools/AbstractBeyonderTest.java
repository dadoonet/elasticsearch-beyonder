/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package fr.pilato.elasticsearch.tools;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assume.assumeThat;

public abstract class AbstractBeyonderTest {

    static final Logger logger = LoggerFactory.getLogger(AbstractBeyonderTest.class);

    private final static String DEFAULT_TEST_CLUSTER = "http://127.0.0.1:9200";
    private final static Integer DEFAULT_TEST_CLUSTER_TRANSPORT_PORT = 9300;

    final static String testCluster = System.getProperty("tests.cluster", DEFAULT_TEST_CLUSTER);
    final static String testClusterUser = System.getProperty("tests.cluster.user");
    final static String testClusterPass = System.getProperty("tests.cluster.pass");
    final static int testClusterTransportPort = Integer.parseInt(System.getProperty("tests.cluster.transport.port", DEFAULT_TEST_CLUSTER_TRANSPORT_PORT.toString()));

    abstract protected void testBeyonder(String root,
                                         List<String> indices,
                                         List<String> templates) throws Exception;

    private static RestClient client;

    static RestClient restClient() throws IOException {
        if (client == null) {
            startRestClient();
        }
        return client;
    }

    private static void startRestClient() throws IOException {
        if (client == null) {
            RestClientBuilder builder = RestClient.builder(HttpHost.create(testCluster));
            if (testClusterUser != null) {
                final CredentialsProvider credentialsProvider =
                        new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(testClusterUser, testClusterPass));
                builder.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                        .setDefaultCredentialsProvider(credentialsProvider));
            }

            client = builder.build();
            testClusterRunning();
        }
    }

    private static boolean testClusterRunning() throws IOException {
        try {
            Response response = client.performRequest(new Request("GET", "/"));
            Map<String, Object> asMap = (Map<String, Object>) JsonUtil.asMap(response).get("version");
            logger.info("Starting integration tests against an external cluster running elasticsearch [{}]", asMap.get("number"));
            return false;
        } catch (ConnectException e) {
            // If we have an exception here, let's ignore the test
            logger.warn("Integration tests are skipped: [{}]", e.getMessage());
            assumeThat("Integration tests are skipped", e.getMessage(), not(containsString("Connection refused")));
            return false;
        } catch (IOException e) {
            logger.error("Full error is", e);
            throw e;
        }
    }

    @Test
    public void testDefaultDir() throws Exception {
        // Default dir es
        testBeyonder(null,
                singletonList("twitter"),
                null);
    }

    @Test
    public void testOneIndexOneType() throws Exception {
        // Single index/single type
        testBeyonder("models/oneindexonetype",
                singletonList("twitter"),
                null);
    }

    @Test
    public void testSettingsAnalyzer() throws Exception {
        // Custom settings (analyzer)
        testBeyonder("models/settingsanalyzer",
                singletonList("twitter"),
                null);
    }

    @Test
    public void testOneIndexNoType() throws Exception {
        // 1 index and no type
        testBeyonder("models/oneindexnotype",
                singletonList("twitter"),
                null);
    }

    @Test
    public void testTemplate() throws Exception {
        // 1 template
        testBeyonder("models/template",
                null,
                singletonList("twitter_template"));
    }

    @Test
    public void testWrongClasspathDir() throws Exception {
        testBeyonder("models/bad-classpath-7/doesnotexist",
                null,
                null);
    }
}
