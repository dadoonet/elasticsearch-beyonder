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
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assume.assumeThat;
import static org.junit.Assume.assumeTrue;

public abstract class AbstractBeyonderTest {

    static final Logger logger = LoggerFactory.getLogger(AbstractBeyonderTest.class);

    private final static String DEFAULT_TEST_CLUSTER_HOST = "127.0.0.1";
    private final static String DEFAULT_TEST_CLUSTER_SCHEME = "http";
    private final static String DEFAULT_USERNAME = "elastic";
    private final static String DEFAULT_PASSWORD = "changeme";
    private final static Integer DEFAULT_TEST_CLUSTER_REST_PORT = 9400;
    private final static Integer DEFAULT_TEST_CLUSTER_TRANSPORT_PORT = 9500;

    final static String testClusterHost = System.getProperty("tests.cluster.host", DEFAULT_TEST_CLUSTER_HOST);
    private final static String testClusterScheme = System.getProperty("tests.cluster.scheme", DEFAULT_TEST_CLUSTER_SCHEME);
    private final static int testClusterRestPort = Integer.parseInt(System.getProperty("tests.cluster.rest.port", DEFAULT_TEST_CLUSTER_REST_PORT.toString()));
    final static int testClusterTransportPort = Integer.parseInt(System.getProperty("tests.cluster.transport.port", DEFAULT_TEST_CLUSTER_TRANSPORT_PORT.toString()));
    final static String testClusterUser = System.getProperty("tests.cluster.user", DEFAULT_USERNAME);
    final static String testClusterPass = System.getProperty("tests.cluster.pass", DEFAULT_PASSWORD);

    abstract protected void testBeyonder(String root,
                                List<String> indices,
                                List<List<String>> types,
                                List<String> templates) throws Exception;

    static boolean securityInstalled;

    private static RestClient client;

    static RestClient restClient() throws IOException {
        if (client == null) {
            startRestClient();
        }
        return client;
    }

    private static void startRestClient() throws IOException {
        if (client == null) {
            client = RestClient.builder(new HttpHost(testClusterHost, testClusterRestPort, testClusterScheme)).build();

            securityInstalled = testClusterRunning(false);
            if (securityInstalled) {
                // We have a secured cluster. So we need to create a secured client
                // But first we need to close the previous client we built
                if (client != null) {
                    client.close();
                }

                final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(testClusterUser, testClusterPass));

                client = RestClient.builder(new HttpHost(testClusterHost, testClusterRestPort, testClusterScheme))
                        .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                            @Override
                            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                                return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                            }
                        })
                        .build();
                securityInstalled = testClusterRunning(true);
            }
        }
    }

    private static boolean testClusterRunning(boolean withSecurity) throws IOException {
        try {
            Response response = client.performRequest("GET", "/");
            Map<String, Object> asMap = (Map<String, Object>) JsonUtil.asMap(response).get("version");

            logger.info("Starting integration tests against an external cluster running elasticsearch [{}] with {}",
                    asMap.get("number"), withSecurity ? "security" : "no security" );
            return withSecurity;
        } catch (ConnectException e) {
            // If we have an exception here, let's ignore the test
            logger.warn("Integration tests are skipped: [{}]", e.getMessage());
            assumeThat("Integration tests are skipped", e.getMessage(), not(containsString("Connection refused")));
            return withSecurity;
        } catch (ResponseException e) {
            if (e.getResponse().getStatusLine().getStatusCode() == 401) {
                logger.debug("The cluster is secured. So we need to build a client with security", e);
                return true;
            } else {
                logger.error("Full error is", e);
                throw e;
            }
        } catch (IOException e) {
            logger.error("Full error is", e);
            throw e;
        }
    }

    static boolean supportsMultipleTypes = true;

    @Test
    public void testDefaultDir() throws Exception {
        // Default dir es
        testBeyonder(null,
                asList("twitter"),
                asList(asList("tweet")),
                null);
    }

    @Test
    public void testOneIndexOneType() throws Exception {
        // Single index/single type
        testBeyonder("models/oneindexonetype",
                asList("twitter"),
                asList(asList("tweet")),
                null);
    }

    @Test
    public void testTwoIndicesTwoTypesOneType() throws Exception {
        assumeTrue("Skipping test as current version does not support multiple types.", supportsMultipleTypes);
        // 2 indices: 2 types and 1 type
        testBeyonder("models/twoindicestwotypesonetype",
                asList("rss", "twitter"),
                asList(asList("doc1", "doc2"), asList("tweet")),
                null);
    }

    @Test
    public void testSettingsAnalyzer() throws Exception {
        // Custom settings (analyzer)
        testBeyonder("models/settingsanalyzer",
                asList("twitter"),
                asList(asList("tweet")),
                null);
    }

    @Test
    public void testOneIndexNoType() throws Exception {
        // 1 index and no type
        testBeyonder("models/oneindexnotype",
                asList("twitter"),
                asList((List<String>) null),
                null);
    }

    @Test
    public void testTemplate() throws Exception {
        // 1 template
        testBeyonder("models/template",
                null,
                null,
                asList("twitter_template"));
    }

    @Test
    public void testUpdateSettings() throws Exception {
        // 1 _update_settings
        testBeyonder("models/update-settings/step1",
                asList("twitter"),
                asList(asList("tweet")),
                null);
        testBeyonder("models/update-settings/step2",
                asList("twitter"),
                asList((List<String>) null),
                null);
    }

    @Test
    public void testWrongClasspathDir() throws Exception {
        testBeyonder("models/bad-classpath-7/doesnotexist",
                null,
                null,
                null);
    }
}
