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

package fr.pilato.elasticsearch.tools.componenttemplate;

import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Manage elasticsearch component templates
 * @author David Pilato
 */
public class ComponentTemplateElasticsearchUpdater {

	private static final Logger logger = LoggerFactory.getLogger(ComponentTemplateElasticsearchUpdater.class);

	/**
	 * Create an component template in Elasticsearch.
	 * @param client Elasticsearch client
	 * @param root dir within the classpath
	 * @param template Template name
	 * @param force set it to true if you want to force cleaning template before adding it
	 * @throws Exception if something goes wrong
	 */
	public static void createComponentTemplate(RestClient client, String root, String template, boolean force) throws Exception {
		String json = ComponentTemplateSettingsReader.readComponentTemplate(root, template);
		createComponentTemplateWithJson(client, template, json, force);
	}

	/**
	 * Create an component template in Elasticsearch. Read read content from default classpath dir.
	 * @param client Elasticsearch client
	 * @param template Template name
	 * @param force set it to true if you want to force cleaning template before adding it
	 * @throws Exception if something goes wrong
	 */
	public static void createComponentTemplate(RestClient client, String template, boolean force) throws Exception {
		String json = ComponentTemplateSettingsReader.readComponentTemplate(template);
		createComponentTemplateWithJson(client, template, json, force);
	}

	/**
	 * Create a new component template in Elasticsearch
	 * @param client Elasticsearch client
	 * @param template Template name
	 * @param json JSon content for the template
	 * @param force set it to true if you want to force cleaning template before adding it
	 * @throws Exception if something goes wrong
	 */
	public static void createComponentTemplateWithJson(RestClient client, String template, String json, boolean force) throws Exception {
		if (isComponentTemplateExist(client, template)) {
			if (force) {
				logger.debug("Component Template [{}] already exists. Force is set. Removing it.", template);
				removeComponentTemplate(client, template);
			} else {
				logger.debug("Component Template [{}] already exists.", template);
			}
		}

		if (!isComponentTemplateExist(client, template)) {
			logger.debug("Component Template [{}] doesn't exist. Creating it.", template);
			createComponentTemplateWithJsonInElasticsearch(client, template, json);
		}
	}

	/**
	 * Create a new component template in Elasticsearch
	 * @param client Elasticsearch client
	 * @param template Template name
	 * @param json JSon content for the template
	 * @throws Exception if something goes wrong
	 */
	private static void createComponentTemplateWithJsonInElasticsearch(RestClient client, String template, String json) throws Exception {
		logger.trace("createComponentTemplate([{}])", template);

		assert client != null;
		assert template != null;

		Request request = new Request("PUT", "/_component_template/" + template);
		request.setJsonEntity(json);
		Response response = client.performRequest(request);

		if (response.getStatusLine().getStatusCode() != 200) {
			logger.warn("Could not create component template [{}]", template);
			throw new Exception("Could not create component template ["+template+"].");
		}

		logger.trace("/createComponentTemplate([{}])", template);
	}

	/**
	 * Check if an component template exists
	 * @param client Elasticsearch client
	 * @param template template name
	 * @return true if the template exists
	 * @throws IOException if something goes wrong
	 */
	public static boolean isComponentTemplateExist(RestClient client, String template) throws IOException {
		Response response = client.performRequest(new Request("HEAD", "/_component_template/" + template));
		return response.getStatusLine().getStatusCode() == 200;
	}

	/**
	 * Remove an component template
	 * @param client Elasticsearch client
	 * @param template template name
	 * @throws Exception if something goes wrong
	 */
	public static void removeComponentTemplate(RestClient client, String template) throws Exception {
		logger.trace("removeComponentTemplate({})", template);
		client.performRequest(new Request("DELETE", "/_component_template/" + template));
		logger.trace("/removeComponentTemplate({})", template);
	}
}
