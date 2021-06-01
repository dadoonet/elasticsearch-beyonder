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

package fr.pilato.elasticsearch.tools.indextemplate;

import fr.pilato.elasticsearch.tools.SettingsFinder;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static fr.pilato.elasticsearch.tools.SettingsReader.getJsonContent;

/**
 * Manage elasticsearch index templates
 * @author David Pilato
 */
public class IndexTemplateElasticsearchUpdater {

	private static final Logger logger = LoggerFactory.getLogger(IndexTemplateElasticsearchUpdater.class);

	/**
	 * Create an index template in Elasticsearch.
	 * @param client Elasticsearch client
	 * @param root dir within the classpath
	 * @param template Template name
	 * @param force set it to true if you want to force cleaning template before adding it
	 * @throws Exception if something goes wrong
	 */
	public static void createIndexTemplate(RestClient client, String root, String template, boolean force) throws Exception {
		String json = getJsonContent(root, SettingsFinder.Defaults.IndexTemplatesDir, template);
		createIndexTemplateWithJson(client, template, json, force);
	}

	/**
	 * Create a new index template in Elasticsearch
	 * @param client Elasticsearch client
	 * @param template Template name
	 * @param json JSon content for the template
	 * @param force set it to true if you want to force cleaning template before adding it
	 * @throws Exception if something goes wrong
	 */
	public static void createIndexTemplateWithJson(RestClient client, String template, String json, boolean force) throws Exception {
		if (isIndexTemplateExist(client, template)) {
			if (force) {
				logger.debug("Index Template [{}] already exists. Force is set. Removing it.", template);
				removeIndexTemplate(client, template);
			} else {
				logger.debug("Index Template [{}] already exists.", template);
			}
		}

		if (!isIndexTemplateExist(client, template)) {
			logger.debug("Index Template [{}] doesn't exist. Creating it.", template);
			createIndexTemplateWithJsonInElasticsearch(client, template, json);
		}
	}

	/**
	 * Create a new index template in Elasticsearch
	 * @param client Elasticsearch client
	 * @param template Template name
	 * @param json JSon content for the template
	 * @throws Exception if something goes wrong
	 */
	private static void createIndexTemplateWithJsonInElasticsearch(RestClient client, String template, String json) throws Exception {
		logger.trace("createIndexTemplate([{}])", template);

		assert client != null;
		assert template != null;

		Request request = new Request("PUT", "/_index_template/" + template);
		request.setJsonEntity(json);
		Response response = client.performRequest(request);

		if (response.getStatusLine().getStatusCode() != 200) {
			logger.warn("Could not create index template [{}]", template);
			throw new Exception("Could not create index template ["+template+"].");
		}

		logger.trace("/createIndexTemplate([{}])", template);
	}

	/**
	 * Check if an index template exists
	 * @param client Elasticsearch client
	 * @param template template name
	 * @return true if the template exists
	 * @throws IOException if something goes wrong
	 */
	public static boolean isIndexTemplateExist(RestClient client, String template) throws IOException {
		Response response = client.performRequest(new Request("HEAD", "/_index_template/" + template));
		return response.getStatusLine().getStatusCode() == 200;
	}

	/**
	 * Remove an index template
	 * @param client Elasticsearch client
	 * @param template template name
	 * @throws Exception if something goes wrong
	 */
	public static void removeIndexTemplate(RestClient client, String template) throws Exception {
		logger.trace("removeIndexTemplate({})", template);
		client.performRequest(new Request("DELETE", "/_index_template/" + template));
		logger.trace("/removeIndexTemplate({})", template);
	}
}
