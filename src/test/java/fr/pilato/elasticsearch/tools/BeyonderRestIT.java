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
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static fr.pilato.elasticsearch.tools.JsonUtil.asMap;
import static fr.pilato.elasticsearch.tools.index.IndexElasticsearchUpdater.isIndexExist;
import static fr.pilato.elasticsearch.tools.template.TemplateElasticsearchUpdater.isTemplateExist;
import static fr.pilato.elasticsearch.tools.type.TypeElasticsearchUpdater.isTypeExist;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
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
            client.performRequest("DELETE", "/_all");
        } catch (IOException e) {
            assumeNoException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractFromPath(Map<String, Object> json, String... path) {
        Map<String, Object> currentObject = json;
        for (String fieldName : path) {
            Object jObject = currentObject.get(fieldName);
            if (jObject == null) {
                throw new RuntimeException("incorrect Json. Was expecting field " + fieldName);
            }
            if (!(jObject instanceof Map)) {
                throw new RuntimeException("incorrect datatype in json. Expected Map and got " + jObject.getClass().getName());
            }
            currentObject = (Map<String, Object>) jObject;
        }
        return currentObject;
    }

    @BeforeClass
    public static void setTestBehavior() {
        try {
            Response response = client.performRequest("GET", "/");
            Map<String, Object> responseAsMap = asMap(response);
            logger.trace("get server response: {}", responseAsMap);
            Object oVersion = extractFromPath(responseAsMap, "version").get("number");
            String version = (String) oVersion;
            if (new VersionComparator().compare(version, "6") > 0) {
                supportsMultipleTypes = false;
            }
        } catch (IOException e) {
            assumeNoException(e);
        }
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

    // This is a manual test as we don't have a real support of this in Beyonder yet
    // See https://github.com/dadoonet/elasticsearch-beyonder/issues/2
    @Test
    public void testAliases() throws Exception {
        IndexElasticsearchUpdater.createIndex(client, "test_aliases", true);
        AliasElasticsearchUpdater.createAlias(client, "foo", "test_aliases");
        Map<String, Object> response = asMap(client.performRequest("GET", "/_alias/foo"));
        assertThat(response, hasKey("test_aliases"));
    }
}
