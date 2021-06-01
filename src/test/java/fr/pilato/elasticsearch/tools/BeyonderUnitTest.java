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

import fr.pilato.elasticsearch.tools.componenttemplate.ComponentTemplateSettingsReader;
import fr.pilato.elasticsearch.tools.index.IndexSettingsReader;
import fr.pilato.elasticsearch.tools.indextemplate.IndexTemplateSettingsReader;
import fr.pilato.elasticsearch.tools.template.TemplateSettingsReader;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static fr.pilato.elasticsearch.tools.index.IndexFinder.findIndexNames;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class BeyonderUnitTest extends AbstractBeyonderTest {

    protected void testBeyonder(String root,
                                List<String> indices,
                                List<String> templates,
                                List<String> componentTemplates,
                                List<String> indexTemplates) throws IOException, URISyntaxException {
        logger.info("--> scanning: [{}]", root);
        List<String> indexNames;
        if (root == null) {
            indexNames = findIndexNames();
        } else {
            indexNames = findIndexNames(root);
        }
        logger.info("  --> indices found: {}", indexNames);

        if (indices != null) {
            assertThat(indexNames, hasSize(indices.size()));

            for (int iIndex = 0; iIndex < indices.size(); iIndex++) {
                String indexName = indexNames.get(iIndex);

                logger.debug("  --> index [{}]:", indexName);
                assertThat(indexName, is(indices.get(iIndex)));

                String settings = IndexSettingsReader.readSettings(root, indexName);
                logger.debug("    --> Settings: [{}]", settings);
            }
        } else {
            assertThat(indexNames, emptyIterable());
        }

        check(ResourceList.getResourceNames(root, SettingsFinder.Defaults.TemplateDir), templates, (name) -> {
            try {
                return TemplateSettingsReader.readTemplate(root, name);
            } catch (IOException e) {
                throw new RuntimeException("Our test is failing...");
            }
        });
        check(ResourceList.getResourceNames(root, SettingsFinder.Defaults.ComponentTemplatesDir), componentTemplates, (name) -> {
            try {
                return ComponentTemplateSettingsReader.readComponentTemplate(root, name);
            } catch (IOException e) {
                throw new RuntimeException("Our test is failing...");
            }
        });
        check(ResourceList.getResourceNames(root, SettingsFinder.Defaults.IndexTemplatesDir), indexTemplates, (name) -> {
            try {
                return IndexTemplateSettingsReader.readIndexTemplate(root, name);
            } catch (IOException e) {
                throw new RuntimeException("Our test is failing...");
            }
        });
    }

    private void check(List<String> names, List<String> expectedNames,
                       Function<String, String> reader) {
        logger.info("  --> names found: {}", names);

        if (expectedNames != null) {
            assertThat(names, hasSize(expectedNames.size()));
            for (int iTemplate = 0; iTemplate < names.size(); iTemplate++) {
                String name = names.get(iTemplate);
                logger.debug("    --> name: [{}]", name);
                assertThat(name, is(expectedNames.get(iTemplate)));

                String json = reader.apply(name);
                logger.debug("      --> Json: [{}]", json);
            }
        } else {
            assertThat(names, emptyIterable());
        }

    }

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
        String numberOfReplicas = (String) ((Map) settingsMap.get("settings")).get("number_of_replicas");
        assertThat(numberOfReplicas, equalTo("2"));

    }

    @Test
    public void testUpdateMapping() throws Exception {
        // 1 _settings
        testBeyonder("models/update-mapping/step1",
                singletonList("twitter"),
                null, null, null);

        // 2 _update_mapping
        testBeyonder("models/update-mapping/step2",
                singletonList("twitter"),
                null, null, null);
    }

    @Test
    public void testUpdateSettings() throws Exception {
        // 1 _settings
        testBeyonder("models/update-settings/step1",
                singletonList("twitter"),
                null, null, null);

        // 2 _update_settings
        testBeyonder("models/update-settings/step2",
                singletonList("twitter"),
                null, null, null);
    }

    @Test
    public void testIndexTemplates() throws Exception {
        // 1 template
        testBeyonder("models/templatev2",
                null,
                null,
                asList("component1", "component2"),
                singletonList("template_1"));
    }
}
