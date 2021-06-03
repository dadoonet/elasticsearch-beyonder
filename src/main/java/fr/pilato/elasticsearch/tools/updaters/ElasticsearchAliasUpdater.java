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

import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticsearchAliasUpdater {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchAliasUpdater.class);

    /**
     * Create an alias if needed
     * @param client Client to use
     * @param alias Alias name
     * @param index Index name
     * @throws Exception When alias can not be set
     */
    @Deprecated
    public static void createAlias(Client client, String alias, String index) throws Exception {
        logger.trace("createAlias({},{})", alias, index);
        AcknowledgedResponse response = client.admin().indices().prepareAliases().addAlias(index, alias).get();
        if (!response.isAcknowledged()) throw new Exception("Could not define alias [" + alias + "] for index [" + index + "].");
        logger.trace("/createAlias({},{})", alias, index);
    }

    /**
     * Create an alias if needed
     * @param client Client to use
     * @param alias Alias name
     * @param index Index name
     * @throws Exception When alias can not be set
     */
    public static void createAlias(RestClient client, String alias, String index) throws Exception {
        logger.trace("createAlias({},{})", alias, index);

        assert client != null;
        assert alias != null;
        assert index != null;

        Request request = new Request("POST", "/_aliases/");
        request.setJsonEntity("{\"actions\":[{\"add\":{\"index\":\"" + index +"\",\"alias\":\"" + alias +"\"}}]}");
        Response response = client.performRequest(request);

        if (response.getStatusLine().getStatusCode() != 200) {
            logger.warn("Could not create alias [{}] on index [{}]", alias, index);
            throw new Exception("Could not create alias ["+alias+"] on index ["+index+"].");
        }
        logger.trace("/createAlias({},{})", alias, index);
    }
}
