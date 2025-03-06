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

import fr.pilato.elasticsearch.tools.util.DefaultSettings;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import static fr.pilato.elasticsearch.tools.util.SettingsReader.getFileContent;

/**
 * Manage elasticsearch data when you want to preload some data
 * @author David Pilato
 */
public class ElasticsearchDataUpdater {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchDataUpdater.class);

	private ElasticsearchDataUpdater() {
		// empty
	}

	/**
	 * Load data from a given ndjson file within the classpath and send it to elasticsearch using the Bulk API.
	 *
	 * @param client    Elasticsearch client
	 * @param root      dir within the classpath
	 * @param index     Index name
	 * @param bulkFiles The list of bulk files to load
	 * @throws Exception if the elasticsearch API call is failing
	 */
	public static void loadBulkData(RestClient client, String root, String index, Collection<String> bulkFiles) throws Exception {
		// If we don't have an index name, we will use _bulk as the subdir to use
		String subdir = index == null ? DefaultSettings.DataDir : index + "/" + DefaultSettings.DataDir;
		for (String bulkFile : bulkFiles) {
			String ndjson = getFileContent(root, subdir, bulkFile);
			if (ndjson != null) {
				logger.debug("Found [{}/{}/{}] file", root, subdir, bulkFile);
				loadBulkDataToElasticsearch(client, index, bulkFile, ndjson);
			}
		}
	}

	/**
	 * Load data from a json file within the classpath and send it to elasticsearch using the Index API (slow).
	 *
	 * @param client    Elasticsearch client
	 * @param root      dir within the classpath
	 * @param index     Index name
	 * @param jsonFiles The list of json files to load
	 * @throws Exception if the elasticsearch API call is failing
	 */
	public static void loadJsonData(RestClient client, String root, String index, Collection<String> jsonFiles) throws Exception {
		// If we don't have an index name, we must fail
		if (index == null) {
			throw new Exception("You must provide an index name when you want to load data from a json file.");
		}

		String subdir = index + "/" + DefaultSettings.DataDir;
		for (String jsonFile : jsonFiles) {
			String json = getFileContent(root, subdir, jsonFile);
			if (json != null) {
				logger.debug("Found [{}/{}/{}] file", root, subdir, jsonFile);
				loadJsonDataToElasticsearch(client, index, jsonFile, json);
			}
		}
	}

	private static void loadBulkDataToElasticsearch(RestClient client, String index, String bulkFile, String ndjson) throws Exception {
		logger.trace("loadBulkDataToElasticsearch([{}], [{}], [{}])", index, bulkFile, ndjson.length());

		assert client != null;

		String endpoint = "/";
		if (index != null) {
			endpoint += index + "/";
		}
		endpoint += "_bulk";

		Request request = new Request("POST", endpoint);
		request.setJsonEntity(ndjson);
		Response response = client.performRequest(request);

		if (response.getStatusLine().getStatusCode() != 200) {
			logger.warn("Could not load bulk file [{}] of size [{}] into Elasticsearch", bulkFile, ndjson.length());
			throw new Exception("Could not load bulk data from file [" + bulkFile + "].");
		}

		logger.trace("/loadBulkDataToElasticsearch([{}], [{}], [{}])", index, bulkFile, ndjson.length());
	}

	private static void loadJsonDataToElasticsearch(RestClient client, String index, String jsonFile, String json) throws Exception {
		logger.trace("loadJsonDataToElasticsearch([{}], [{}], [{}])", index, jsonFile, json.length());

		assert client != null;

		String endpoint = "/" + index + "/_doc/";
		Request request = new Request("POST", endpoint);
		request.setJsonEntity(json);
		Response response = client.performRequest(request);

		if (response.getStatusLine().getStatusCode() != 201) {
			logger.warn("Could not load json file [{}] of size [{}] into Elasticsearch", jsonFile, json.length());
			throw new Exception("Could not load json data from file [" + jsonFile + "].");
		}

		logger.trace("/loadJsonDataToElasticsearch([{}], [{}], [{}])", index, jsonFile, json.length());
	}
}
