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

import fr.pilato.elasticsearch.tools.ResourceList;
import fr.pilato.elasticsearch.tools.SettingsFinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;

public class IndexFinder extends SettingsFinder {
    private static final Logger logger = LogManager.getLogger(IndexFinder.class);

    /**
     * Find all indices existing in a given default classpath dir
     */
    public static List<String> findIndexNames() throws IOException {
        return findIndexNames(Defaults.ConfigDir);
    }

    /**
     * Find all indices existing in a given classpath dir
     * @param root dir within the classpath
     */
    public static List<String> findIndexNames(final String root) throws IOException {
        if (root == null) {
            return findIndexNames();
        }

        logger.debug("Looking for indices in classpath under [{}].", root);

        final List<String> indexNames = new ArrayList<>();
        final Set<String> keys = new HashSet<>();
        Collection<String> resources = ResourceList.getResources(root + "/"); // "es/" or "a/b/c/"
        for (String resource : resources) {
            logger.trace(" - resource [{}].", resource);
            String withoutRoot = resource.substring(root.length()+1);
            logger.trace(" - withoutRoot [{}].", withoutRoot);
            String key;
            if (withoutRoot.indexOf("/") >= 0) {
                key = withoutRoot.substring(0, withoutRoot.indexOf("/"));
            } else {
                key = withoutRoot;
            }
            if (!key.equals(Defaults.TemplateDir) && !keys.contains(key)) {
                logger.trace(" - found [{}].", key);
                keys.add(key);
                indexNames.add(key);
            }
        }

        return indexNames;
    }
}
