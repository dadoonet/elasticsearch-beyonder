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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static fr.pilato.elasticsearch.tools.index.IndexFinder.findIndexNames;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

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
}
