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

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assume.assumeThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpHost;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import fr.pilato.elasticsearch.tools.index.IndexSettingsReader;

public abstract class AbstractBeyonderTest {

    static final Logger logger = LoggerFactory.getLogger(AbstractBeyonderTest.class);

    abstract protected void testBeyonder(String root,
                                         List<String> indices,
                                         List<String> templates) throws Exception;

	protected static ElasticsearchContainer container;
	protected static RestClient				client;
	protected static Client					transportClient;

	@BeforeClass
	public static void initilizeElasticSearch() throws IOException, InterruptedException {
		container = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.7.1");
		container.start();
		client = RestClient.builder(HttpHost.create(container.getHttpHostAddress())).build();
		TransportAddress transportAddress = new TransportAddress(container.getTcpHost());
		Settings settings = Settings.builder().put("cluster.name", "docker-cluster").build();
		transportClient = new PreBuiltTransportClient(settings).addTransportAddress(transportAddress);
		testClusterRunning();
	}

	@SuppressWarnings("unchecked")
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
	
	@AfterClass
	public static void tearDownElasticSearch() {
		if (container != null)
			container.stop();
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

    @SuppressWarnings("unchecked")
	@Test
    public void testVariableReplacement() throws Exception {

      // given: A settings json with a variable that should be replaced.
      //        And an environment variable with matching name (set in configuration of maven-surefire-plugin).
      String folder = "models/variablereplacement";
      String indexName = "twitter";

      // when: this settings file is read
      String settings = IndexSettingsReader.readSettings(folder, indexName);
      Map<String, Object> settingsMap = JsonUtil.asMap(new ByteArrayInputStream(settings.getBytes()));

      // then: the variables got replaced by environment variables of the same name
      String numberOfReplicas = (String) ((Map<String, Object>) settingsMap.get("settings")).get("number_of_replicas");
      assumeThat(numberOfReplicas, equalTo("2"));

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
    public void testUpdateSettings() throws Exception {
        // 1 _update_settings
        testBeyonder("models/update-settings/step1",
                singletonList("twitter"),
                null);
        testBeyonder("models/update-settings/step2",
                singletonList("twitter"),
                null);
    }

    @Test
    public void testWrongClasspathDir() throws Exception {
        testBeyonder("models/bad-classpath-7/doesnotexist",
                null,
                null);
    }

}
