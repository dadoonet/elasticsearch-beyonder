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

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static fr.pilato.elasticsearch.tools.JsonUtil.asMap;
import static fr.pilato.elasticsearch.tools.updaters.ElasticsearchIndexUpdater.isIndexExist;
import static fr.pilato.elasticsearch.tools.updaters.ElasticsearchPipelineUpdater.isPipelineExist;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assume.assumeNoException;

public class BeyonderRestIT extends AbstractBeyonderTest {

    private static final Logger logger = LoggerFactory.getLogger(BeyonderRestIT.class);

    private static final String DEFAULT_TEST_CLUSTER = "https://127.0.0.1:9200";
    private static final String DEFAULT_TEST_USER = "elastic";
    private static final String DEFAULT_TEST_PASSWORD = "changeme";

    private static final String testCluster = System.getProperty("tests.cluster", DEFAULT_TEST_CLUSTER);
    private static final String testClusterUser = System.getProperty("tests.cluster.user", DEFAULT_TEST_USER);
    private static final String testClusterPass = System.getProperty("tests.cluster.pass", DEFAULT_TEST_PASSWORD);

    private static RestClient client;

    static RestClient restClient() throws IOException {
        if (client == null) {
            startRestClient();
        }
        return client;
    }

    private static void startRestClient() throws IOException {
        if (client == null) {
            client = buildClient(testCluster, testClusterUser, null);
            try {
                ConnectException connectException = testClusterRunning();
                if (connectException != null) {
                    if (testCluster.equals(DEFAULT_TEST_CLUSTER)) {
                        Properties props = new Properties();
                        props.load(TestContainerHelper.class.getResourceAsStream("/beyonder-tests.properties"));
                        String version = props.getProperty("elasticsearch.version");

                        TestContainerHelper containerHelper = new TestContainerHelper();
                        String url = containerHelper.startElasticsearch(version, testClusterPass);
                        Path clusterCaCrtPath = null;
                        if (containerHelper.getCertAsBytes() != null) {
                            clusterCaCrtPath = rootTmpDir.resolve("cluster-ca.crt");
                            Files.write(clusterCaCrtPath, containerHelper.getCertAsBytes());
                        }
                        client = buildClient(url, DEFAULT_TEST_USER, clusterCaCrtPath);
                        connectException = testClusterRunning();
                        if (connectException != null) {
                            throw new IOException(connectException);
                        }
                    } else {
                        throw new IOException(connectException);
                    }
                }
            } catch (IOException e) {
                logger.error("Can not connect to [{}]: {}", testCluster, e.getMessage());
                throw e;
            }
        }
    }

    private static ConnectException testClusterRunning() throws IOException {
        try {
            Response response = client.performRequest(new Request("GET", "/"));
            @SuppressWarnings("unchecked")
            Map<String, Object> asMap = (Map<String, Object>) JsonUtil.asMap(response).get("version");
            logger.info("Starting integration tests against an external cluster running elasticsearch [{}]", asMap.get("number"));
            return null;
        } catch (ConnectException e) {
            return e;
        }
    }

    private static RestClient buildClient(String url, String user, Path caCertificate) {
        RestClientBuilder builder = RestClient.builder(HttpHost.create(url));
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, testClusterPass));
        builder.setHttpClientConfigCallback(hcb -> {
                    hcb.setDefaultCredentialsProvider(credentialsProvider);
                    if (caCertificate == null) {
                        hcb.setSSLContext(SSLUtils.yesSSLContext());
                    } else {
                        hcb.setSSLContext(SSLUtils.sslContextFromHttpCaCrt(caCertificate));
                    }
                    return hcb;
                }
        );
        return builder.build();
    }


    @BeforeClass
    public static void startElasticsearchRestClient() throws IOException {
        client = restClient();
    }

    @AfterClass
    public static void stopElasticsearchRestClient() throws IOException {
        if (client != null) {
            client.close();
        }
    }

    @Before @After
    public void cleanCluster() {
        // DELETE /twitter
        launchAndIgnoreFailure(() -> client.performRequest(new Request("DELETE", "/twitter")));
        // DELETE /test_*
        launchAndIgnoreFailure(() -> client.performRequest(new Request("DELETE", "/test_*")));
        // DELETE /person
        launchAndIgnoreFailure(() -> client.performRequest(new Request("DELETE", "/person")));
        // DELETE /my-index-*
        launchAndIgnoreFailure(() -> client.performRequest(new Request("DELETE", "/my-index-*")));
        // DELETE /timeseries-*
        launchAndIgnoreFailure(() -> client.performRequest(new Request("DELETE", "/timeseries-*")));

        // DELETE /_ingest/pipeline/twitter_pipeline
        launchAndIgnoreFailure(() -> client.performRequest(new Request("DELETE", "/_ingest/pipeline/twitter_pipeline")));

        // DELETE /_template/twitter_template
        launchAndIgnoreFailure(() -> client.performRequest(new Request("DELETE", "/_template/twitter_template")));

        // DELETE /_index_template/template_1
        launchAndIgnoreFailure(() -> client.performRequest(new Request("DELETE", "/_index_template/template_1")));

        // DELETE /_component_template/component1
        launchAndIgnoreFailure(() -> client.performRequest(new Request("DELETE", "/_component_template/component1")));
        // DELETE /_component_template/component2
        launchAndIgnoreFailure(() -> client.performRequest(new Request("DELETE", "/_component_template/component2")));

        // DELETE /_ilm/policy/index_lifecycle
        launchAndIgnoreFailure(() -> client.performRequest(new Request("DELETE", "/_ilm/policy/index_lifecycle")));
    }

    @BeforeClass
    public static void setTestBehavior() {
        try {
            Response response = client.performRequest(new Request("GET", "/"));
            Map<String, Object> responseAsMap = asMap(response);
            logger.trace("get server response: {}", responseAsMap);
        } catch (IOException e) {
            assumeNoException(e);
        }
    }

    protected void testBeyonder(String root,
                                List<String> indices,
                                List<String> componentTemplates,
                                List<String> indexTemplates,
                                List<String> pipelines,
                                List<String> indexLifecycles) throws Exception {
        logger.info("--> scanning: [{}]", root);
        ElasticsearchBeyonder.start(client, root);

        // We can now check if we have the component templates created
        if (componentTemplates != null) {
            boolean allExists = true;

            for (String template : componentTemplates) {
                if (!existObjectInElasticsearch("/_component_template/" + template)) {
                    allExists = false;
                }
            }
            assertThat(allExists, is(true));
        }

        // We can now check if we have the index templates created
        if (indexTemplates != null) {
            boolean allExists = true;

            for (String template : indexTemplates) {
                if (!existObjectInElasticsearch("/_index_template/" + template)) {
                    allExists = false;
                }
            }
            assertThat(allExists, is(true));
        }

        // We can now check if we have the indices created
        if (indices != null) {
            boolean allExists = true;

            for (String index : indices) {
                if (!isIndexExist(client, index)) {
                    allExists = false;
                }
            }
            assertThat(allExists, is(true));
        }

        // We can check if we have created the pipelines
        if (pipelines != null) {
            boolean allExists = true;

            for (String pipeline : pipelines) {
                if (!isPipelineExist(client, pipeline)) {
                    allExists = false;
                }
            }
            assertThat(allExists, is(true));
        }

        // We can check if we have the index lifecycle policies created
        if (indexLifecycles != null) {
            boolean allExists = true;

            for (String policy : indexLifecycles) {
                if (!existObjectInElasticsearch("/_ilm/policy/" + policy, "GET")) {
                    allExists = false;
                }
            }
            assertThat(allExists, is(true));
        }
    }

    private boolean existObjectInElasticsearch(String url) throws IOException {
        return existObjectInElasticsearch(url, "HEAD");
    }

    private boolean existObjectInElasticsearch(String url, String method) throws IOException {
        return client.performRequest(new Request(method, url)).getStatusLine().getStatusCode() == 200;
    }

    @Test
    public void testAliases() throws Exception {
        ElasticsearchBeyonder.start(client, "models/aliases");
        Map<String, Object> response = asMap(client.performRequest(new Request("GET", "/_alias/test")));
        assertThat(response, not(hasKey("test_1")));
        assertThat(response, hasKey("test_2"));
    }

    @Test
    public void testMergeDisabled() throws Exception {
        ElasticsearchBeyonder.start(client);
        String expectedMapping = getMapping("twitter");
        ElasticsearchBeyonder.start(client, "models/mergedisabled", false);
        String actualMapping = getMapping("twitter");
        assertThat(actualMapping, is(expectedMapping));
    }

    @Test
    public void testForceEnabled() throws Exception {
        ElasticsearchBeyonder.start(client);
        String oldMapping = getMapping("twitter");
        ElasticsearchBeyonder.start(client, "models/forceenabled", true);
        String newMapping = getMapping("twitter");
        assertThat(newMapping, is(not(oldMapping)));
    }
    
    @Test
	public void testPipelines() throws Exception {
        testBeyonder("models/pipelines", null, null, null, singletonList("twitter_pipeline"), null);
	}

    @Test
    public void testUpdateSettings() throws Exception {
        // 1 _settings
        {
            testBeyonder("models/update-settings/step1",
                    singletonList("twitter"),
                    null, null, null, null);
            Map<String, Object> oldSettings = asMap(client.performRequest(new Request("GET", "/twitter/_settings")));
            String numberOfReplicas = BeanUtils.getProperty(oldSettings, "twitter.settings.index.number_of_replicas");
            assertThat(numberOfReplicas, equalTo("0"));
        }

        // 2 _update_settings
        {
            testBeyonder("models/update-settings/step2",
                    singletonList("twitter"),
                    null, null, null, null);
            Map<String, Object> settings = asMap(client.performRequest(new Request("GET", "/twitter/_settings")));
            String numberOfReplicas = BeanUtils.getProperty(settings, "twitter.settings.index.number_of_replicas");
            assertThat(numberOfReplicas, equalTo("1"));
        }
    }

    @Test
    public void testUpdateMapping() throws Exception {
        // 1 _settings
        {
            testBeyonder("models/update-mapping/step1",
                    singletonList("twitter"),
                    null, null, null, null);

            Map<String, Object> mapping = asMap(client.performRequest(new Request("GET", "/twitter/_mappings")));
            String bar = BeanUtils.getProperty(mapping, "twitter.mappings.properties.bar");
            String foo = BeanUtils.getProperty(mapping, "twitter.mappings.properties.foo");
            String message = BeanUtils.getProperty(mapping, "twitter.mappings.properties.message.search_analyzer");
            assertThat(bar, nullValue());
            assertThat(foo, notNullValue());
            assertThat(message, nullValue());
        }

        // 2 _update_mapping
        {
            testBeyonder("models/update-mapping/step2",
                    singletonList("twitter"),
                    null, null, null, null);

            Map<String, Object> mapping = asMap(client.performRequest(new Request("GET", "/twitter/_mappings")));
            String bar = BeanUtils.getProperty(mapping, "twitter.mappings.properties.bar");
            String foo = BeanUtils.getProperty(mapping, "twitter.mappings.properties.foo");
            String message = BeanUtils.getProperty(mapping, "twitter.mappings.properties.message.search_analyzer");
            assertThat(bar, notNullValue());
            assertThat(foo, notNullValue());
            assertThat(message, notNullValue());
        }
    }

    @Test
    public void testIndexTemplates() throws Exception {
        // 1 template
        testBeyonder("models/templatev2",
                null,
                asList("component1", "component2"),
                singletonList("template_1"), null, null);
    }

    @Test
    public void testIndexLifecycles() throws Exception {
        // 1 policy
        testBeyonder("models/index-lifecycle",
                null,
                null,
                null,
                null,
                singletonList("index_lifecycle"));
    }

    @Test
    public void testDataBulkAndJsonGlobalWithIndices() throws Exception {
        // 2 indices with 10 documents + 1 index with 4 documents + 1 global bulk file with 10 documents
        testBeyonder("models/data-bulk-and-json-global-with-indices",
                asList("person", "twitter", "test_1", "test_2"),
                null, null, null, null);

        // Refresh the indices
        client.performRequest(new Request("POST", "/_refresh"));

        // Check that we have 5 documents in person index. This is coming from the 4 person json files
        {
            Map<String, Object> response = asMap(client.performRequest(new Request("GET", "/person/_search")));
            String numberOfHits = BeanUtils.getProperty(response, "hits.total.value");
            assertThat(numberOfHits, equalTo("4"));
        }

        // Check that we have 10 documents in twitter index. This is coming from the global bulk file
        {
            Map<String, Object> response = asMap(client.performRequest(new Request("GET", "/twitter/_search")));
            String numberOfHits = BeanUtils.getProperty(response, "hits.total.value");
            assertThat(numberOfHits, equalTo("10"));
        }

        // Check that we have 10 documents in test_1 index
        {
            Map<String, Object> response = asMap(client.performRequest(new Request("GET", "/test_1/_search")));
            String numberOfHits = BeanUtils.getProperty(response, "hits.total.value");
            assertThat(numberOfHits, equalTo("10"));
        }

        // Check that we have 10 documents in test_2 index
        {
            Map<String, Object> response = asMap(client.performRequest(new Request("GET", "/test_2/_search")));
            String numberOfHits = BeanUtils.getProperty(response, "hits.total.value");
            assertThat(numberOfHits, equalTo("10"));
        }
    }

    @Test
    public void testDataGlobal() throws Exception {
        // 1 index with 10 documents
        testBeyonder("models/data-global",
                singletonList("twitter"),
                null, null, null, null);

        // Refresh the indices
        client.performRequest(new Request("POST", "/_refresh"));

        // Check that we have 10 documents in twitter index
        Map<String, Object> response = asMap(client.performRequest(new Request("GET", "/twitter/_search")));
        String numberOfHits = BeanUtils.getProperty(response, "hits.total.value");
        assertThat(numberOfHits, equalTo("10"));
    }

    @Test
    public void testDataGlobalWithIndices() throws Exception {
        // 2 indices with 10 documents + 1 global bulk file with 10 documents
        testBeyonder("models/data-global-with-indices",
                asList("twitter", "test_1", "test_2"),
                null, null, null, null);

        // Refresh the indices
        client.performRequest(new Request("POST", "/_refresh"));

        // Check that we have 10 documents in twitter index. This is coming from the global bulk file
        {
            Map<String, Object> response = asMap(client.performRequest(new Request("GET", "/twitter/_search")));
            String numberOfHits = BeanUtils.getProperty(response, "hits.total.value");
            assertThat(numberOfHits, equalTo("10"));
        }

        // Check that we have 10 documents in test_1 index
        {
            Map<String, Object> response = asMap(client.performRequest(new Request("GET", "/test_1/_search")));
            String numberOfHits = BeanUtils.getProperty(response, "hits.total.value");
            assertThat(numberOfHits, equalTo("10"));
        }

        // Check that we have 10 documents in test_2 index
        {
            Map<String, Object> response = asMap(client.performRequest(new Request("GET", "/test_2/_search")));
            String numberOfHits = BeanUtils.getProperty(response, "hits.total.value");
            assertThat(numberOfHits, equalTo("10"));
        }
    }

    @Test
    public void testDataMoreIndices() throws Exception {
        // 2 indices with 10 documents each
        testBeyonder("models/data-more-indices",
                asList("test_1", "test_2"),
                null, null, null, null);

        // Refresh the indices
        client.performRequest(new Request("POST", "/_refresh"));

        // Check that we have 10 documents in test_1 index
        {
            Map<String, Object> response = asMap(client.performRequest(new Request("GET", "/test_1/_search")));
            String numberOfHits = BeanUtils.getProperty(response, "hits.total.value");
            assertThat(numberOfHits, equalTo("10"));
        }

        // Check that we have 10 documents in test_2 index
        {
            Map<String, Object> response = asMap(client.performRequest(new Request("GET", "/test_2/_search")));
            String numberOfHits = BeanUtils.getProperty(response, "hits.total.value");
            assertThat(numberOfHits, equalTo("10"));
        }
    }

    @Test
    public void testDataOneIndex() throws Exception {
        // 1 index with 10 documents
        testBeyonder("models/data-one-index",
                singletonList("twitter"),
                null, null, null, null);

        // Refresh the indices
        client.performRequest(new Request("POST", "/_refresh"));

        // Check that we have 10 documents in twitter index
        Map<String, Object> response = asMap(client.performRequest(new Request("GET", "/twitter/_search")));
        String numberOfHits = BeanUtils.getProperty(response, "hits.total.value");
        assertThat(numberOfHits, equalTo("10"));
    }

    @Test
    public void testDataShouldNotBeLoadedTwice() throws Exception {
        // 2 indices with 10 documents each
        testBeyonder("models/data-more-indices",
                asList("test_1", "test_2"),
                null, null, null, null);

        // Refresh the indices
        client.performRequest(new Request("POST", "/_refresh"));

        // Check that we have 10 documents in test_1 index
        {
            Map<String, Object> response = asMap(client.performRequest(new Request("GET", "/test_1/_search")));
            String numberOfHits = BeanUtils.getProperty(response, "hits.total.value");
            assertThat(numberOfHits, equalTo("10"));
        }

        // Check that we have 10 documents in test_2 index
        {
            Map<String, Object> response = asMap(client.performRequest(new Request("GET", "/test_2/_search")));
            String numberOfHits = BeanUtils.getProperty(response, "hits.total.value");
            assertThat(numberOfHits, equalTo("10"));
        }

        // We execute it again, which should not add more documents
        testBeyonder("models/data-more-indices",
                asList("test_1", "test_2"),
                null, null, null, null);

        // Refresh the indices
        client.performRequest(new Request("POST", "/_refresh"));

        // Check that we have 10 documents in test_1 index
        {
            Map<String, Object> response = asMap(client.performRequest(new Request("GET", "/test_1/_search")));
            String numberOfHits = BeanUtils.getProperty(response, "hits.total.value");
            assertThat(numberOfHits, equalTo("10"));
        }

        // Check that we have 10 documents in test_2 index
        {
            Map<String, Object> response = asMap(client.performRequest(new Request("GET", "/test_2/_search")));
            String numberOfHits = BeanUtils.getProperty(response, "hits.total.value");
            assertThat(numberOfHits, equalTo("10"));
        }
    }

    @Test
    public void testDataOnForcedIndexShouldBeLoadedTwice() throws Exception {
        // 1 index with 10 documents
        testBeyonder("models/data-one-index",
                singletonList("twitter"),
                null, null, null, null);

        // Refresh the indices
        client.performRequest(new Request("POST", "/_refresh"));

        // Check that we have 10 documents in twitter index
        {
            Map<String, Object> response = asMap(client.performRequest(new Request("GET", "/twitter/_search")));
            String numberOfHits = BeanUtils.getProperty(response, "hits.total.value");
            assertThat(numberOfHits, equalTo("10"));
        }

        // We execute it again, which should now add more documents as we are forcing the recreation of the indices
        ElasticsearchBeyonder.start(client, "models/data-one-index", true);

        // Refresh the indices
        client.performRequest(new Request("POST", "/_refresh"));

        // Check that we have 10 documents in twitter index
        {
            Map<String, Object> response = asMap(client.performRequest(new Request("GET", "/twitter/_search")));
            String numberOfHits = BeanUtils.getProperty(response, "hits.total.value");
            assertThat(numberOfHits, equalTo("10"));
        }
    }

    @Test
    public void testDateMathIndices() throws Exception {
        testBeyonder("models/date-math-indices",
                singletonList("my-index-*"),
                null, null, null, null);

        // Refresh the indices
        client.performRequest(new Request("POST", "/_refresh"));

        // Check that we have 10 documents in my-index-* index
        Map<String, Object> response = asMap(client.performRequest(new Request("GET", "/my-index-*/_search")));
        String numberOfHits = BeanUtils.getProperty(response, "hits.total.value");
        assertThat(numberOfHits, equalTo("10"));
    }

    @Test
    public void testDateMathIndicesWithExistingDailyIndex() throws Exception {
        // Manually create an index named <my-index-{now/d}>
        client.performRequest(new Request("PUT", "/%3Cmy-index-%7Bnow%2Fd%7D%3E"));

        testBeyonder("models/date-math-indices",
                singletonList("my-index-*"),
                null, null, null, null);

        // Refresh the indices
        client.performRequest(new Request("POST", "/_refresh"));

        // Check that we have 0 documents in my-index-* index because the index was already existing
        Map<String, Object> response = asMap(client.performRequest(new Request("GET", "/my-index-*/_search")));
        String numberOfHits = BeanUtils.getProperty(response, "hits.total.value");
        assertThat(numberOfHits, equalTo("0"));
    }

    @Test
    public void testDateMathIndicesWithExistingOlderIndex() throws Exception {
        // Manually create an index named <my-index-{now/d-1d}>
        client.performRequest(new Request("PUT", "/%3Cmy-index-%7Bnow%2Fd-1d%7D%3E"));

        testBeyonder("models/date-math-indices",
                singletonList("my-index-*"),
                null, null, null, null);

        // Refresh the indices
        client.performRequest(new Request("POST", "/_refresh"));

        // Expected outcome is:
        // Index from yesterday should exist but with no documents
        {
            Map<String, Object> response = asMap(client.performRequest(new Request("GET", "/%3Cmy-index-%7Bnow%2Fd-1d%7D%3E/_search")));
            String numberOfHits = BeanUtils.getProperty(response, "hits.total.value");
            assertThat(numberOfHits, equalTo("0"));
        }

        // Index from today should not have been created as we do have yet an older index with the same date math
        {
            Map<String, Object> response = asMap(client.performRequest(new Request("GET", "/my-index-*/_search")));
            String numberOfHits = BeanUtils.getProperty(response, "hits.total.value");
            assertThat(numberOfHits, equalTo("0"));
        }
    }

    @Test
    public void testRolloverSimple() throws Exception {
        testBeyonder("models/rollover-simple",
                singletonList("timeseries-000001"),
                null, null, null, null);

        // Manually trigger the rollover
        client.performRequest(new Request("POST", "/timeseries/_rollover"));

        // Post another document to trigger rollover
        Request postDoc = new Request("POST", "/timeseries/_doc");
        postDoc.setJsonEntity("{\"message\":\"message 2\"}");
        client.performRequest(postDoc);

        // Refresh the indices
        client.performRequest(new Request("POST", "/_refresh"));

        // Check that we have 2 documents in timeseries index
        {
            Map<String, Object> response = asMap(client.performRequest(new Request("GET", "/timeseries/_search")));
            String numberOfHits = BeanUtils.getProperty(response, "hits.total.value");
            assertThat(numberOfHits, equalTo("2"));
        }

        // Delete the first index to simulate an ILM DELETE phase
        client.performRequest(new Request("DELETE", "/timeseries-000001"));

        // Check that we have 1 document in timeseries index
        {
            Map<String, Object> response = asMap(client.performRequest(new Request("GET", "/timeseries/_search")));
            String numberOfHits = BeanUtils.getProperty(response, "hits.total.value");
            assertThat(numberOfHits, equalTo("1"));
        }

        // Call Beyonder again and it should not fail
        testBeyonder("models/rollover-simple",
                singletonList("timeseries-000002"),
                null, null, null, null);

        // We should not have the timeseries-000001 index
        assertThat(client.performRequest(new Request("HEAD", "/timeseries-000001"))
                .getStatusLine().getStatusCode(), not(200));

        // Check that we still have only 1 document in timeseries index
        {
            Map<String, Object> response = asMap(client.performRequest(new Request("GET", "/timeseries/_search")));
            String numberOfHits = BeanUtils.getProperty(response, "hits.total.value");
            assertThat(numberOfHits, equalTo("1"));
        }
    }

    @Test
    public void testRolloverWithDateMaths() throws Exception {
        testBeyonder("models/rollover-date-maths",
                singletonList("timeseries-*-000001"),
                null, null, null, null);

        // Manually trigger the rollover
        client.performRequest(new Request("POST", "/timeseries/_rollover"));

        // Post another document to trigger rollover
        Request postDoc = new Request("POST", "/timeseries/_doc");
        postDoc.setJsonEntity("{\"message\":\"message 2\"}");
        client.performRequest(postDoc);

        // Refresh the indices
        client.performRequest(new Request("POST", "/_refresh"));

        // Check that we have 2 documents in timeseries index
        {
            Map<String, Object> response = asMap(client.performRequest(new Request("GET", "/timeseries/_search")));
            String numberOfHits = BeanUtils.getProperty(response, "hits.total.value");
            assertThat(numberOfHits, equalTo("2"));
        }

        // Delete the first index to simulate an ILM DELETE phase
        client.performRequest(new Request("DELETE", "/timeseries-*-000001"));

        // Check that we have 1 document in timeseries index
        {
            Map<String, Object> response = asMap(client.performRequest(new Request("GET", "/timeseries/_search")));
            String numberOfHits = BeanUtils.getProperty(response, "hits.total.value");
            assertThat(numberOfHits, equalTo("1"));
        }

        // Call Beyonder again and it should not fail
        testBeyonder("models/rollover-simple",
                singletonList("timeseries-*-000002"),
                null, null, null, null);

        // We should not have the timeseries-*-000001 index
        String responseBody = new BufferedReader(new InputStreamReader(
                client.performRequest(new Request("GET", "/timeseries-*-000001")).getEntity().getContent()))
                .lines()
                .reduce("", (accumulator, actual) -> accumulator + actual);
        assertThat(responseBody, is("{}"));

        // Check that we still have only 1 document in timeseries index
        {
            Map<String, Object> response = asMap(client.performRequest(new Request("GET", "/timeseries/_search")));
            String numberOfHits = BeanUtils.getProperty(response, "hits.total.value");
            assertThat(numberOfHits, equalTo("1"));
        }
    }

    private String getMapping(String indexName) throws IOException {
        HttpEntity response = client.performRequest(new Request("GET", "/" + indexName + "/_mapping")).getEntity();
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.toIntExact(response.getContentLength() > 0 ? response.getContentLength() : 4000L));
        IOUtils.copy(response.getContent(), out);
        return out.toString();
    }
}
