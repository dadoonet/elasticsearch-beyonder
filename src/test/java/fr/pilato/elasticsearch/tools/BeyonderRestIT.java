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

import fr.pilato.elasticsearch.tools.alias.AliasElasticsearchUpdater;
import fr.pilato.elasticsearch.tools.index.IndexElasticsearchUpdater;
import fr.pilato.elasticsearch.tools.pipeline.PipelineElasticsearchUpdater;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
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
import static fr.pilato.elasticsearch.tools.index.IndexElasticsearchUpdater.isIndexExist;
import static fr.pilato.elasticsearch.tools.template.TemplateElasticsearchUpdater.isTemplateExist;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
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

    @Before
    public void cleanCluster() {
        try {
            client.performRequest(new Request("DELETE", "/_all"));
        } catch (IOException e) {
            assumeNoException(e);
        }
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
                                List<String> templates) throws Exception {
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
    }

    // This is a manual test as we don't have a real support of this in Beyonder yet
    // See https://github.com/dadoonet/elasticsearch-beyonder/issues/2
    @Test
    public void testAliases() throws Exception {
        IndexElasticsearchUpdater.createIndex(client, "test_aliases", true);
        AliasElasticsearchUpdater.createAlias(client, "foo", "test_aliases");
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
		assertThat(PipelineElasticsearchUpdater.isPipelineExist(client, "twitter_pipeline"), is(true));
	}

    private String getMapping(String indexName) throws IOException {
        HttpEntity response = client.performRequest(new Request("GET", indexName + "/_mapping")).getEntity();
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.toIntExact(response.getContentLength()));
        IOUtils.copy(response.getContent(), out);
        return new String(out.toByteArray());
    }
}
