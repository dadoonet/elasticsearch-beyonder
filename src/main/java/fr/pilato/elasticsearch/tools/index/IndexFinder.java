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

package fr.pilato.elasticsearch.tools.index;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.pilato.elasticsearch.tools.ResourceList;
import fr.pilato.elasticsearch.tools.SettingsFinder;

public class IndexFinder extends SettingsFinder {
    private static final Logger logger = LoggerFactory.getLogger(IndexFinder.class);

    /**
     * Find all indices existing in a given default classpath dir
     * @return a list of indices
     * @throws IOException if connection with elasticsearch is failing
     * @throws URISyntaxException this should not happen
     */
    public static List<String> findIndexNames() throws IOException, URISyntaxException {
        return findIndexNames(Defaults.ConfigDir);
    }

    /**
     * Find all indices existing in a given classpath dir
     * @param root dir within the classpath
     * @return a list of indices
     * @throws IOException if connection with elasticsearch is failing
     * @throws URISyntaxException this should not happen
     */
    public static List<String> findIndexNames(final String root) throws IOException, URISyntaxException {
        if (root == null) {
            return findIndexNames();
        }

        logger.debug("Looking for indices in classpath under [{}].", root);

        final List<String> indexNames = new ArrayList<>();
        final Set<String> keys = new HashSet<>();
        String[] resources = ResourceList.getResources(root + "/"); // "es/" or "a/b/c/"
        for (String resource : resources) {
            if (!resource.isEmpty()) {
                logger.trace(" - resource [{}].", resource);
                String key;
                if (resource.contains("/")) {
                    key = resource.substring(0, resource.indexOf("/"));
                } else {
                    key = resource;
                }
				if (!key.equals(Defaults.TemplateDir) && !key.equals(Defaults.PipelineDir) && !keys.contains(key)) {
                    logger.trace(" - found [{}].", key);
                    keys.add(key);
                    indexNames.add(key);
                }
            }
        }

        return indexNames;
    }
}
