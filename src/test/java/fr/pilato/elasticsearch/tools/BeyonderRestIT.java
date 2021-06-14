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

import fr.pilato.elasticsearch.tools.updaters.ElasticsearchAliasUpdater;
import fr.pilato.elasticsearch.tools.updaters.ElasticsearchIndexUpdater;
import fr.pilato.elasticsearch.tools.updaters.ElasticsearchPipelineUpdater;
import fr.pilato.elasticsearch.tools.util.SettingsFinder;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static fr.pilato.elasticsearch.tools.JsonUtil.asMap;
import static fr.pilato.elasticsearch.tools.updaters.ElasticsearchComponentTemplateUpdater.isComponentTemplateExist;
import static fr.pilato.elasticsearch.tools.updaters.ElasticsearchIndexTemplateUpdater.isIndexTemplateExist;
import static fr.pilato.elasticsearch.tools.updaters.ElasticsearchIndexUpdater.isIndexExist;
import static fr.pilato.elasticsearch.tools.updaters.ElasticsearchPipelineUpdater.isPipelineExist;
import static fr.pilato.elasticsearch.tools.updaters.ElasticsearchTemplateUpdater.isTemplateExist;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assume.assumeNoException;

public class BeyonderRestIT extends AbstractBeyonderTest {

    private static final Logger logger = LoggerFactory.getLogger(BeyonderRestIT.class);
    private static RestClient client;

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
    public void cleanCluster() throws Exception {
        // DELETE /twitter
        launchAndIgnoreFailure(() -> client.performRequest(new Request("DELETE", "/twitter")));
        // DELETE /test_aliases
        launchAndIgnoreFailure(() -> client.performRequest(new Request("DELETE", "/test_aliases")));

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
                                List<String> templates,
                                List<String> componentTemplates,
                                List<String> indexTemplates,
                                List<String> pipelines) throws Exception {
        logger.info("--> scanning: [{}]", root);
        ElasticsearchBeyonder.start(client, root);

        // We can now check if we have the templates created
        if (templates != null) {
            boolean allExists = true;

            for (String template : templates) {
                if (!isTemplateExist(client, template)) {
                    allExists = false;
                }
            }
            assertThat(allExists, is(true));
        }

        // We can now check if we have the component templates created
        if (componentTemplates != null) {
            boolean allExists = true;

            for (String template : componentTemplates) {
                if (!isComponentTemplateExist(client, template)) {
                    allExists = false;
                }
            }
            assertThat(allExists, is(true));
        }

        // We can now check if we have the index templates created
        if (indexTemplates != null) {
            boolean allExists = true;

            for (String template : indexTemplates) {
                if (!isIndexTemplateExist(client, template)) {
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
    }

    // This is a manual test as we don't have a real support of this in Beyonder yet
    // See https://github.com/dadoonet/elasticsearch-beyonder/issues/2
    @Test
    public void testAliases() throws Exception {
        ElasticsearchIndexUpdater.createIndex(client, SettingsFinder.Defaults.ConfigDir, "test_aliases", true);
        ElasticsearchAliasUpdater.createAlias(client, "foo", "test_aliases");
        Map<String, Object> response = asMap(client.performRequest(new Request("GET", "/_alias/foo")));
        assertThat(response, hasKey("test_aliases"));
    }

    @Test
    public void testMergeDisabled() throws Exception {
        ElasticsearchBeyonder.start(client);
        String expectedMapping = getMapping("twitter");
        ElasticsearchBeyonder.start(client, "models/mergedisabled", false, false);
        String actualMapping = getMapping("twitter");
        assertThat(actualMapping, is(expectedMapping));
    }

    @Test
    public void testForceEnabled() throws Exception {
        ElasticsearchBeyonder.start(client);
        String oldMapping = getMapping("twitter");
        ElasticsearchBeyonder.start(client, "models/forceenabled", true, true);
        String newMapping = getMapping("twitter");
        assertThat(newMapping, is(not(oldMapping)));
    }
    
    @Test
	public void testPipeline() throws Exception {
		ElasticsearchBeyonder.start(client, "models/pipeline");
		assertThat(isPipelineExist(client, "twitter_pipeline"), is(true));
	}

    @Test
	public void testPipelines() throws Exception {
        testBeyonder("models/pipelines", null, null, null, null, singletonList("twitter_pipeline"));
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
                null,
                asList("component1", "component2"),
                singletonList("template_1"), null);
    }


    private String getMapping(String indexName) throws IOException {
        HttpEntity response = client.performRequest(new Request("GET", indexName + "/_mapping")).getEntity();
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.toIntExact(response.getContentLength()));
        IOUtils.copy(response.getContent(), out);
        return new String(out.toByteArray());
    }
}
