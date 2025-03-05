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

package fr.pilato.elasticsearch.tools.updaters;

import fr.pilato.elasticsearch.tools.util.SettingsFinder;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static fr.pilato.elasticsearch.tools.util.SettingsReader.getJsonContent;

public class ElasticsearchAliasUpdater {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchAliasUpdater.class);

    /**
     * Manage global aliases in Elasticsearch.
     * @param client Elasticsearch client
     * @param root dir within the classpath
     * @throws Exception if something goes wrong
     */
    public static void manageAliases(RestClient client, String root) throws Exception {
        String json = getJsonContent(root, null, SettingsFinder.Defaults.AliasesFile);
        if (json != null) {
            logger.debug("Found [{}/{}.{}] file", root, SettingsFinder.Defaults.AliasesFile, SettingsFinder.Defaults.JsonFileExtension);
            manageAliasesWithJsonInElasticsearch(client, json);
        }
    }

    /**
     *
     * @param client Client to use
     * @param json JSon content for the aliases
     * @throws Exception if something goes wrong
     */
    private static void manageAliasesWithJsonInElasticsearch(RestClient client, String json) throws Exception {
        logger.trace("manageAliases()");

        assert client != null;
        assert json != null;

        Request request = new Request("POST", "/_aliases/");
        request.setJsonEntity(json);
        Response response = client.performRequest(request);

        if (response.getStatusLine().getStatusCode() != 200) {
            logger.warn("Could not manage aliases. Got error: {}: {}",
                    response.getStatusLine().getStatusCode(),
                    response.getStatusLine().getReasonPhrase());
            throw new Exception("Could not manage aliases.");
        }
        logger.trace("/manageAliases()");
    }
}
