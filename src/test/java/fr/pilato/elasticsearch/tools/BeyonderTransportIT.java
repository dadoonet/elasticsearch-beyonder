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

import org.elasticsearch.Version;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import static fr.pilato.elasticsearch.tools.index.IndexElasticsearchUpdater.isIndexExist;
import static fr.pilato.elasticsearch.tools.template.TemplateElasticsearchUpdater.isTemplateExist;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeNoException;

public class BeyonderTransportIT extends AbstractBeyonderTest {

    private static final Logger logger = LoggerFactory.getLogger(BeyonderTransportIT.class);
    private static Client client;

    @BeforeClass
    public static void startElasticsearch() throws IOException {
        // This is going to initialize our Rest Client if not initialized yet
        restClient();
        client = new PreBuiltTransportClient(Settings.builder().put("client.transport.ignore_cluster_name", true).build())
                .addTransportAddress(new TransportAddress(new InetSocketAddress(testClusterHost, testClusterTransportPort)));
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
            NodesInfoResponse response = client.admin().cluster().prepareNodesInfo().get();
            for (NodeInfo nodeInfo : response.getNodes()) {
                Version version = nodeInfo.getVersion();
                if (version.id >= 6000000) {
                    supportsMultipleTypes = false;
                }
            }
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
}
