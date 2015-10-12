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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static fr.pilato.elasticsearch.tools.index.IndexElasticsearchUpdater.isIndexExist;
import static fr.pilato.elasticsearch.tools.template.TemplateElasticsearchUpdater.isTemplateExist;
import static fr.pilato.elasticsearch.tools.type.TypeElasticsearchUpdater.isTypeExist;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class BeyonderIntegrationTest extends AbstractBeyonderTest {

    private static final Logger logger = LogManager.getLogger(BeyonderIntegrationTest.class);
    private static Node node;
    private static Client client;
    private static File testDir;

    private static void recursiveDelete(File file) {
        File[] files = file.listFiles();
        if (files != null) {
            for (File each : files) {
                recursiveDelete(each);
            }
        }
        file.delete();
    }

    @BeforeClass
    public static void startElasticsearch() throws IOException {
        testDir = File.createTempFile("junit", "");
        testDir.delete();
        testDir.mkdir();
        node = NodeBuilder.nodeBuilder().settings(Settings.builder()
            .put("path.home", testDir.getAbsolutePath()).build()
        ).node();
        client = node.client();
    }

    @AfterClass
    public static void stopElasticsearch() {
        if (client != null) {
            client.close();
        }
        if (node != null) {
            node.close();
        }
        recursiveDelete(testDir);
    }

    @Before
    public void cleanCluster() {
        client.admin().indices().prepareDelete("_all").get();
    }

    protected void testBeyonder(String root,
                             List<String> indices,
                             List<List<String>> types,
                             List<String> templates) throws Exception {
        logger.info("--> scanning: [{}]", SettingsFinder.fromClasspath(root));
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

            for (int iIndex = 0; iIndex < indices.size(); iIndex++) {
                if (types != null && types.get(iIndex) != null) {
                        for (String type : types.get(iIndex)) {
                        boolean exists = isTypeExist(client, indices.get(iIndex), type);
                        assertThat("type " + type + " should exist in index " + indices.get(iIndex),
                                exists, is(true));
                    }
                }
            }
        }
    }
}
