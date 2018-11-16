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

package fr.pilato.elasticsearch.tools.template;

import fr.pilato.elasticsearch.tools.ResourceList;
import fr.pilato.elasticsearch.tools.SettingsFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class TemplateFinder extends SettingsFinder {
    private static final Logger logger = LoggerFactory.getLogger(TemplateFinder.class);

    /**
     * Find all templates in default classpath dir
     * @return a list of templates
     * @throws IOException if connection with elasticsearch is failing
     * @throws URISyntaxException this should not happen
     */
    public static List<String> findTemplates() throws IOException, URISyntaxException {
        return findTemplates(Defaults.ConfigDir);
    }

    /**
     * Find all templates
     * @param root dir within the classpath
     * @return a list of templates
     * @throws IOException if connection with elasticsearch is failing
     * @throws URISyntaxException this should not happen
     */
    public static List<String> findTemplates(String root) throws IOException, URISyntaxException {
        if (root == null) {
            return findTemplates();
        }

        logger.debug("Looking for templates in classpath under [{}].", root);

        final List<String> templateNames = new ArrayList<>();
        String[] resources = ResourceList.getResources(root + "/" + Defaults.TemplateDir + "/"); // "es/_template/"
        for (String resource : resources) {
            if (!resource.isEmpty()) {
                String withoutIndex = resource.substring(resource.indexOf("/")+1);
                String template = withoutIndex.substring(0, withoutIndex.indexOf(Defaults.JsonFileExtension));
                logger.trace(" - found [{}].", template);
                templateNames.add(template);
            }
        }

        return templateNames;
    }
}
