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

import fr.pilato.elasticsearch.tools.index.IndexSettingsReader;
import fr.pilato.elasticsearch.tools.template.TemplateFinder;
import fr.pilato.elasticsearch.tools.template.TemplateSettingsReader;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import static fr.pilato.elasticsearch.tools.index.IndexFinder.findIndexNames;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class BeyonderUnitTest extends AbstractBeyonderTest {

    protected void testBeyonder(String root,
                                List<String> indices,
                                List<String> templates) throws IOException, URISyntaxException {
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

        List<String> templateNames = TemplateFinder.findTemplates(root);
        logger.info("  --> templates found: {}", templateNames);

        if (templates != null) {
            assertThat(templateNames, hasSize(templates.size()));
            for (int iTemplate = 0; iTemplate < templateNames.size(); iTemplate++) {
                String templateName = templateNames.get(iTemplate);
                logger.debug("    --> template: [{}]", templateName);
                assertThat(templateName, is(templates.get(iTemplate)));

                String template = TemplateSettingsReader.readTemplate(root, templateName);
                logger.debug("      --> Template: [{}]", template);
            }
        } else {
            assertThat(templateNames, emptyIterable());
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
                null);

        // 2 _update_mapping
        testBeyonder("models/update-mapping/step2",
                singletonList("twitter"),
                null);
    }

    @Test
    public void testUpdateSettings() throws Exception {
        // 1 _settings
        testBeyonder("models/update-settings/step1",
                singletonList("twitter"),
                null);

        // 2 _update_settings
        testBeyonder("models/update-settings/step2",
                singletonList("twitter"),
                null);
    }
}
