/*
 * Licensed to David Pilato (the "Author") under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Author licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
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

/**
 * Manage elasticsearch index lifecycles
 */
public class ElasticsearchIndexLifecycleUpdater {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchIndexLifecycleUpdater.class);

	/**
	 * Create an index lifecycle policy in Elasticsearch.
	 * @param client Elasticsearch client
	 * @param root dir within the classpath
	 * @param policy Lifecycle policy name
	 * @throws Exception if something goes wrong
	 */
	public static void createIndexLifecycle(RestClient client, String root, String policy) throws Exception {
		String json = getJsonContent(root, SettingsFinder.Defaults.IndexLifecyclesDir, policy);
		createIndexLifecycleWithJsonInElasticsearch(client, policy, json);
	}

	/**
	 * Create an index lifecycle policy in Elasticsearch.
	 * @param client Elasticsearch client
	 * @param policy Lifecycle policy name
	 * @param json JSon content for the policy
	 * @throws Exception if something goes wrong
	 */
	private static void createIndexLifecycleWithJsonInElasticsearch(RestClient client, String policy, String json) throws Exception {
		logger.trace("createIndexLifecycle([{}])", policy);

		assert client != null;
		assert policy != null;

		Request request = new Request("PUT", "/_ilm/policy/" + policy);
		request.setJsonEntity(json);
		Response response = client.performRequest(request);

		if (response.getStatusLine().getStatusCode() != 200) {
			logger.warn("Could not create index lifecycle policy [{}]", policy);
			throw new Exception("Could not create index lifecycle policy ["+policy+"].");
		}

		logger.trace("/createIndexTemplate([{}])", policy);
	}
}
