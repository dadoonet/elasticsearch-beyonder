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
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static fr.pilato.elasticsearch.tools.util.SettingsReader.getJsonContent;

/**
 * Manage elasticsearch templates
 * @author David Pilato
 */
@Deprecated
public class ElasticsearchTemplateUpdater {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchTemplateUpdater.class);

	/**
	 * Create a template in Elasticsearch.
	 * @param client Elasticsearch client
	 * @param root dir within the classpath
	 * @param template Template name
	 * @throws Exception if something goes wrong
	 * @deprecated Will be removed when we don't support TransportClient anymore
	 */
	@Deprecated
	public static void createTemplate(Client client, String root, String template) throws Exception {
		String json = getJsonContent(root, SettingsFinder.Defaults.TemplateDir, template);
		createTemplateWithJsonInElasticsearch(client, template, json);
	}

	/**
	 * Create a new index in Elasticsearch
	 * @param client Elasticsearch client
	 * @param template Template name
	 * @param json JSon content for the template
     * @throws Exception if something goes wrong
     * @deprecated Will be removed when we don't support TransportClient anymore
	 */
	@Deprecated
	private static void createTemplateWithJsonInElasticsearch(Client client, String template, String json) throws Exception {
		logger.trace("createTemplate([{}])", template);

		assert client != null;
		assert template != null;

        AcknowledgedResponse response = client.admin().indices()
				.preparePutTemplate(template)
				.setSource(json.getBytes(), XContentType.JSON)
				.get();

		if (!response.isAcknowledged()) {
			logger.warn("Could not create template [{}]", template);
			throw new Exception("Could not create template ["+template+"].");
		}

		logger.trace("/createTemplate([{}])", template);
	}

	/**
	 * Create a legacy template in Elasticsearch.
	 * @param client Elasticsearch client
	 * @param root dir within the classpath
	 * @param template Template name
     * @throws Exception if something goes wrong
	 */
	@Deprecated
	public static void createTemplate(RestClient client, String root, String template) throws Exception {
		String json = getJsonContent(root, SettingsFinder.Defaults.TemplateDir, template);
		createTemplateWithJsonInElasticsearch(client, template, json);
	}

	/**
	 * Create a new legacy template in Elasticsearch
	 * @param client Elasticsearch client
	 * @param template Template name
	 * @param json JSon content for the template
	 * @param force set it to true if you want to force cleaning template before adding it
     * @throws Exception if something goes wrong
	 */
	@Deprecated
	public static void createTemplateWithJson(RestClient client, String template, String json, boolean force) throws Exception {
		createTemplateWithJsonInElasticsearch(client, template, json);
	}

	/**
	 * Create a new legacy template in Elasticsearch
	 * @param client Elasticsearch client
	 * @param template Template name
	 * @param json JSon content for the template
     * @throws Exception if something goes wrong
	 */
	@Deprecated
	private static void createTemplateWithJsonInElasticsearch(RestClient client, String template, String json) throws Exception {
		logger.trace("createTemplate([{}])", template);

		assert client != null;
		assert template != null;

		Request request = new Request("PUT", "/_template/" + template);
		request.setJsonEntity(json);
		Response response = client.performRequest(request);

		if (response.getStatusLine().getStatusCode() != 200) {
			logger.warn("Could not create template [{}]", template);
			throw new Exception("Could not create template ["+template+"].");
		}

		logger.trace("/createTemplate([{}])", template);
	}

	/**
	 * Check if a legacy template exists
     * @param client Elasticsearch client
	 * @param template template name
     * @return true if the template exists
     * @throws IOException if something goes wrong
	 */
	@Deprecated
	public static boolean isTemplateExist(RestClient client, String template) throws IOException {
		Response response = client.performRequest(new Request("HEAD", "/_template/" + template));
		return response.getStatusLine().getStatusCode() == 200;
	}

	/**
	 * Remove a legacy template
     * @param client Elasticsearch client
	 * @param template template name
     * @throws Exception if something goes wrong
	 */
	@Deprecated
	public static void removeTemplate(RestClient client, String template) throws Exception {
		logger.trace("removeTemplate({})", template);
		client.performRequest(new Request("DELETE", "/_template/" + template));
		logger.trace("/removeTemplate({})", template);
	}

}
