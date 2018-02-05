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

package fr.pilato.elasticsearch.tools.template;

import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;

/**
 * Manage elasticsearch templates
 * @author David Pilato
 */
public class TemplateElasticsearchUpdater {

	private static final Logger logger = LoggerFactory.getLogger(TemplateElasticsearchUpdater.class);

	/**
	 * Create a template in Elasticsearch.
	 * @param client Elasticsearch client
	 * @param root dir within the classpath
	 * @param template Template name
	 * @throws Exception
	 */
	@Deprecated
	public static void createTemplate(Client client, String root, String template, boolean force) throws Exception {
		String json = TemplateSettingsReader.readTemplate(root, template);
		createTemplateWithJson(client, template, json, force);
	}

	/**
	 * Create a template in Elasticsearch. Read read content from default classpath dir.
	 * @param client Elasticsearch client
	 * @param template Template name
	 * @throws Exception
	 */
	@Deprecated
	public static void createTemplate(Client client, String template, boolean force) throws Exception {
		String json = TemplateSettingsReader.readTemplate(template);
		createTemplateWithJson(client, template, json, force);
	}

	/**
	 * Create a new template in Elasticsearch
	 * @param client Elasticsearch client
	 * @param template Template name
	 * @param json JSon content for the template
	 * @param force set it to true if you want to force cleaning template before adding it
	 * @throws Exception
	 */
	@Deprecated
	public static void createTemplateWithJson(Client client, String template, String json, boolean force) throws Exception {
		if (isTemplateExist(client, template)) {
			if (force) {
				logger.debug("Template [{}] already exists. Force is set. Removing it.", template);
				removeTemplate(client, template);
			} else {
				logger.debug("Template [{}] already exists.", template);
			}
		}

		if (!isTemplateExist(client, template)) {
			logger.debug("Template [{}] doesn't exist. Creating it.", template);
			createTemplateWithJsonInElasticsearch(client, template, json);
		}
	}

	/**
	 * Create a new index in Elasticsearch
	 * @param client Elasticsearch client
	 * @param template Template name
	 * @param json JSon content for the template
	 * @throws Exception
	 */
	@Deprecated
	private static void createTemplateWithJsonInElasticsearch(Client client, String template, String json) throws Exception {
		logger.trace("createTemplate([{}])", template);

		assert client != null;
		assert template != null;

		PutIndexTemplateResponse response = client.admin().indices()
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
	 * Check if a template exists
	 * @param template template name
	 */
	@Deprecated
	public static boolean isTemplateExist(Client client, String template) {
		return !client.admin().indices().prepareGetTemplates(template).get().getIndexTemplates().isEmpty();
	}

	/**
	 * Remove a template
	 * @param template
	 * @throws Exception
	 */
	@Deprecated
	public static void removeTemplate(Client client, String template) throws Exception {
		logger.trace("removeTemplate({})", template);
		client.admin().indices().prepareDeleteTemplate(template).get();
		logger.trace("/removeTemplate({})", template);
	}

	/**
	 * Create a template in Elasticsearch.
	 * @param client Elasticsearch client
	 * @param root dir within the classpath
	 * @param template Template name
	 * @throws Exception
	 */
	public static void createTemplate(RestClient client, String root, String template, boolean force) throws Exception {
		String json = TemplateSettingsReader.readTemplate(root, template);
		createTemplateWithJson(client, template, json, force);
	}

	/**
	 * Create a template in Elasticsearch. Read read content from default classpath dir.
	 * @param client Elasticsearch client
	 * @param template Template name
	 * @throws Exception
	 */
	public static void createTemplate(RestClient client, String template, boolean force) throws Exception {
		String json = TemplateSettingsReader.readTemplate(template);
		createTemplateWithJson(client, template, json, force);
	}

	/**
	 * Create a new template in Elasticsearch
	 * @param client Elasticsearch client
	 * @param template Template name
	 * @param json JSon content for the template
	 * @param force set it to true if you want to force cleaning template before adding it
	 * @throws Exception
	 */
	public static void createTemplateWithJson(RestClient client, String template, String json, boolean force) throws Exception {
		if (isTemplateExist(client, template)) {
			if (force) {
				logger.debug("Template [{}] already exists. Force is set. Removing it.", template);
				removeTemplate(client, template);
			} else {
				logger.debug("Template [{}] already exists.", template);
			}
		}

		if (!isTemplateExist(client, template)) {
			logger.debug("Template [{}] doesn't exist. Creating it.", template);
			createTemplateWithJsonInElasticsearch(client, template, json);
		}
	}

	/**
	 * Create a new index in Elasticsearch
	 * @param client Elasticsearch client
	 * @param template Template name
	 * @param json JSon content for the template
	 * @throws Exception
	 */
	private static void createTemplateWithJsonInElasticsearch(RestClient client, String template, String json) throws Exception {
		logger.trace("createTemplate([{}])", template);

		assert client != null;
		assert template != null;

		Response response = client.performRequest("PUT", "/_template/" + template, Collections.<String, String>emptyMap(),
				new StringEntity(json, ContentType.APPLICATION_JSON));


		if (response.getStatusLine().getStatusCode() != 200) {
			logger.warn("Could not create template [{}]", template);
			throw new Exception("Could not create template ["+template+"].");
		}

		logger.trace("/createTemplate([{}])", template);
	}

	/**
	 * Check if a template exists
	 * @param template template name
	 */
	public static boolean isTemplateExist(RestClient client, String template) throws IOException {
		Response response = client.performRequest("HEAD", "/_template/" + template);
		return response.getStatusLine().getStatusCode() == 200;
	}

	/**
	 * Remove a template
	 * @param template
	 * @throws Exception
	 */
	public static void removeTemplate(RestClient client, String template) throws Exception {
		logger.trace("removeTemplate({})", template);
		client.performRequest("DELETE", "/_template/" + template);
		logger.trace("/removeTemplate({})", template);
	}

}
