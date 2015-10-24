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

package fr.pilato.elasticsearch.tools.index;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.client.Client;

/**
 * Manage elasticsearch index settings
 * @author David Pilato
 */
public class IndexElasticsearchUpdater {

	private static final Logger logger = LogManager.getLogger(IndexElasticsearchUpdater.class);

	/**
	 * Create a new index in Elasticsearch. Read also _settings.json if exists.
	 * @param client Elasticsearch client
	 * @param root dir within the classpath
	 * @param index Index name
	 * @param force Remove index if exists (Warning: remove all data)
	 * @throws Exception
	 */
	public static void createIndex(Client client, String root, String index, boolean force) throws Exception {
		String settings = IndexSettingsReader.readSettings(root, index);
		createIndexWithSettings(client, index, settings, force);
	}

	/**
	 * Create a new index in Elasticsearch. Read also _settings.json if exists in default classpath dir.
	 * @param client Elasticsearch client
	 * @param index Index name
	 * @param force Remove index if exists (Warning: remove all data)
	 * @throws Exception
	 */
	public static void createIndex(Client client, String index, boolean force) throws Exception {
		String settings = IndexSettingsReader.readSettings(index);
		createIndexWithSettings(client, index, settings, force);
	}

	/**
	 * Create a new index in Elasticsearch
	 * @param client Elasticsearch client
	 * @param index Index name
	 * @param settings Settings if any, null if no specific settings
	 * @param force Remove index if exists (Warning: remove all data)
	 * @throws Exception
	 */
	public static void createIndexWithSettings(Client client, String index, String settings, boolean force) throws Exception {
		if (force && isIndexExist(client, index)) {
			logger.debug("Index [{}] already exists but force set to true. Removing all data!", index);
			removeIndexInElasticsearch(client, index);
		}
		if (force || !isIndexExist(client, index)) {
			logger.debug("Index [{}] doesn't exist. Creating it.", index);
			createIndexWithSettingsInElasticsearch(client, index, settings);
		} else {
			logger.debug("Index [{}] already exists.", index);
		}
	}

	/**
	 * Remove a new index in Elasticsearch
	 * @param client Elasticsearch client
	 * @param index Index name
	 * @throws Exception
	 */
	private static void removeIndexInElasticsearch(Client client, String index) throws Exception {
		logger.trace("removeIndex([{}])", index);

		assert client != null;
		assert index != null;

		DeleteIndexResponse deleteIndexResponse = client.admin().indices().prepareDelete(index).get();
		if (!deleteIndexResponse.isAcknowledged()) {
			logger.warn("Could not delete index [{}]", index);
			throw new Exception("Could not delete index ["+index+"].");
		}

		logger.trace("/removeIndex([{}])", index);
	}

	/**
	 * Create a new index in Elasticsearch
	 * @param client Elasticsearch client
	 * @param index Index name
	 * @param settings Settings if any, null if no specific settings
	 * @throws Exception
	 */
	private static void createIndexWithSettingsInElasticsearch(Client client, String index, String settings) throws Exception {
		logger.trace("createIndex([{}])", index);

		assert client != null;
		assert index != null;

		CreateIndexRequestBuilder cirb = client.admin().indices().prepareCreate(index);

		// If there are settings for this index, we use it. If not, using Elasticsearch defaults.
		if (settings != null) {
			logger.trace("Found settings for index [{}]: [{}]", index, settings);
			cirb.setSettings(settings);
		}

		CreateIndexResponse createIndexResponse = cirb.execute().actionGet();
		if (!createIndexResponse.isAcknowledged()) {
			logger.warn("Could not create index [{}]", index);
			throw new Exception("Could not create index ["+index+"].");
		}

		logger.trace("/createIndex([{}])", index);
	}

	/**
	 * Update settings in Elasticsearch
	 * @param client Elasticsearch client
	 * @param index Index name
	 * @param settings Settings if any, null if no update settings
	 * @throws Exception
	 */
	private static void updateIndexWithSettingsInElasticsearch(Client client, String index, String settings) throws Exception {
		logger.trace("updateIndex([{}])", index);

		assert client != null;
		assert index != null;

		if (settings != null) {
			logger.trace("Found update settings for index [{}]: [{}]", index, settings);
			logger.debug("updating settings for index [{}]", index);
			client.admin().indices().prepareUpdateSettings(index).setSettings(settings).get();
		}

		logger.trace("/updateIndex([{}])", index);
	}

	/**
	 * Check if an index already exists
	 * @param index Index name
	 * @return true if index already exists
	 * @throws Exception
	 */
	public static boolean isIndexExist(Client client, String index) throws Exception {
		return client.admin().indices().prepareExists(index).get().isExists();
	}

	/**
	 * Update index settings in Elasticsearch. Read also _update_settings.json if exists.
	 * @param client Elasticsearch client
	 * @param root dir within the classpath
	 * @param index Index name
	 * @throws Exception
	 */
	public static void updateSettings(Client client, String root, String index) throws Exception {
		String settings = IndexSettingsReader.readUpdateSettings(root, index);
		updateIndexWithSettingsInElasticsearch(client, index, settings);
	}

	/**
	 * Update index settings in Elasticsearch. Read also _update_settings.json if exists in default classpath dir.
	 * @param client Elasticsearch client
	 * @param index Index name
	 * @throws Exception
	 */
	public static void updateSettings(Client client, String index) throws Exception {
		String settings = IndexSettingsReader.readUpdateSettings(index);
		updateIndexWithSettingsInElasticsearch(client, index, settings);
	}
}
