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

import static fr.pilato.elasticsearch.tools.index.IndexElasticsearchUpdater.isIndexExist;
import static fr.pilato.elasticsearch.tools.template.TemplateElasticsearchUpdater.isTemplateExist;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
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

    protected void testBeyonder(String root,
                                List<String> indices,
                                List<String> templates) throws Exception {
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
