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
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

import static fr.pilato.elasticsearch.tools.index.IndexElasticsearchUpdater.isIndexExist;
import static fr.pilato.elasticsearch.tools.template.TemplateElasticsearchUpdater.isTemplateExist;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assume.assumeNoException;
import static org.junit.Assume.assumeTrue;

public class BeyonderTransportIT extends AbstractBeyonderTest {

    private static final Logger logger = LoggerFactory.getLogger(BeyonderTransportIT.class);
    private static Client client;

    @BeforeClass
    public static void startElasticsearch() throws IOException {
        // If a user is defined, we just ignore the tests as we did not bring in the deprecated
        // transport secured client
        assumeTrue("Transport tests are not run on a secured cluster", testClusterUser == null);
        // This is going to initialize our Rest Client if not initialized yet
        RestClient restClient = restClient();
        client = new PreBuiltTransportClient(Settings.builder().put("client.transport.ignore_cluster_name", true).build())
                .addTransportAddress(new TransportAddress(new InetSocketAddress(restClient.getNodes().get(0).getHost().getHostName(), testClusterTransportPort)));
    }

    @AfterClass
    public static void stopElasticsearch() {
        if (client != null) {
            client.close();
        }
    }

    @BeforeClass
    public static void setTestBehavior() {
        try {
            client.admin().cluster().prepareNodesInfo().get();
        } catch (NoNodeAvailableException e) {
            assumeNoException(e);
        }
    }

    @Before
    public void cleanCluster() {
        try {
            client.admin().indices().prepareDelete("_all").get();
        } catch (NoNodeAvailableException e) {
            assumeNoException(e);
        }
    }

    @Override
    @Test
    public void testDefaultDir() {
        assumeTrue("We skip the default dir test for transport client as the index " +
                "settings format is not compatible with rest client (type needs to " +
                "be provided)", false);
    }

    @Test
    public void testUpdateSettings() throws Exception {
        // 1 _settings
        {
            testBeyonder("models/update-settings/step1",
                    singletonList("twitter"),
                    null, null, null);
            GetSettingsResponse settings = client.admin().indices().prepareGetSettings("twitter").get();
            String numberOfReplicas = settings.getSetting("twitter", "index.number_of_replicas");
            assertThat(numberOfReplicas, equalTo("0"));
        }

        // 2 _update_settings
        {
            testBeyonder("models/update-settings/step2",
                    singletonList("twitter"),
                    null, null, null);
            GetSettingsResponse settings = client.admin().indices().prepareGetSettings("twitter").get();
            String numberOfReplicas = settings.getSetting("twitter", "index.number_of_replicas");
            assertThat(numberOfReplicas, equalTo("1"));
        }
    }

    @Test
    public void testUpdateMapping() throws Exception {
        // 1 _settings
        {
            testBeyonder("models/update-mapping/step1",
                    singletonList("twitter"),
                    null, null, null);
            Map<String, Object> properties = client.admin().indices().prepareGetMappings("twitter").get().getMappings().get("twitter").get("_doc").getSourceAsMap();
            String bar = BeanUtils.getProperty(properties, "properties.bar");
            String foo = BeanUtils.getProperty(properties, "properties.foo");
            String message = BeanUtils.getProperty(properties, "properties.message.search_analyzer");
            assertThat(bar, nullValue());
            assertThat(foo, notNullValue());
            assertThat(message, nullValue());
        }

        // 2 _update_mapping
        {
            testBeyonder("models/update-mapping/step2",
                    singletonList("twitter"),
                    null, null, null);

            Map<String, Object> properties = client.admin().indices().prepareGetMappings("twitter").get().getMappings().get("twitter").get("_doc").getSourceAsMap();
            String bar = BeanUtils.getProperty(properties, "properties.bar");
            String foo = BeanUtils.getProperty(properties, "properties.foo");
            String message = BeanUtils.getProperty(properties, "properties.message.search_analyzer");
            assertThat(bar, notNullValue());
            assertThat(foo, notNullValue());
            assertThat(message, notNullValue());
        }
    }

    protected void testBeyonder(String root,
                                List<String> indices,
                                List<String> templates,
                                List<String> componentTemplates,
                                List<String> indexTemplates) throws Exception {
        String newRoot = "transport/" + root;
        logger.info("--> scanning: [{}]", newRoot);
        ElasticsearchBeyonder.start(client, newRoot);

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
}
