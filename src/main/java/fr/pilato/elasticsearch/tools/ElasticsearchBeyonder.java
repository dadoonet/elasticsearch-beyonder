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

package fr.pilato.elasticsearch.tools;

import fr.pilato.elasticsearch.tools.SettingsFinder.Defaults;
import fr.pilato.elasticsearch.tools.index.IndexFinder;
import fr.pilato.elasticsearch.tools.template.TemplateFinder;
import fr.pilato.elasticsearch.tools.type.TypeFinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.Client;

import java.util.Collection;
import java.util.List;

import static fr.pilato.elasticsearch.tools.SettingsFinder.fromClasspath;
import static fr.pilato.elasticsearch.tools.index.IndexElasticsearchUpdater.createIndex;
import static fr.pilato.elasticsearch.tools.index.IndexElasticsearchUpdater.updateSettings;
import static fr.pilato.elasticsearch.tools.template.TemplateElasticsearchUpdater.createTemplate;
import static fr.pilato.elasticsearch.tools.type.TypeElasticsearchUpdater.createMapping;

/**
 * By default, indexes are created with their default Elasticsearch settings. You can specify
 * your own settings for your index by putting a /es/indexname/_settings.json in your classpath.
 * <br>
 * So if you create a file named /es/twitter/_settings.json in your src/main/resources folder (for maven lovers),
 * it will be used by the factory to create the twitter index.
 * <pre>
 * {@code
 * {
 *   "index" : {
 *     "number_of_shards" : 3,
 *     "number_of_replicas" : 2
 *   }
 * }
 * </pre>
 * By default, types are not created and wait for the first document you send to Elasticsearch (auto mapping).
 * But, if you define a file named /es/indexname/type.json in your classpath, the type will be created at startup using
 * the type definition you give.
 * <br>
 * So if you create a file named /es/twitter/tweet.json in your src/main/resources folder (for maven lovers),
 * it will be used by the factory to create the tweet type in twitter index.
 * <pre>
 * {@code
 * {
 *   "tweet" : {
 *     "properties" : {
 *       "message" : {"type" : "string", "store" : "yes"}
 *     }
 *   }
 * }
 * </pre>
 *
 * By convention, the factory will create all settings and mappings found under the /es classpath.<br>
 * You can disable convention and use configuration by setting autoscan to false.
 * @author David Pilato
 */
public class ElasticsearchBeyonder {

	private static final Logger logger = LogManager.getLogger(ElasticsearchBeyonder.class);

	/**
	 * Automatically scan classpath and creates indices, types, templates... in default dir.
	 * @param client elasticsearch client
	 */
	public static void start(Client client) throws Exception {
		start(client, fromClasspath(Defaults.ConfigDir));
	}

	/**
	 * Automatically scan classpath and creates indices, types, templates...
	 * @param client elasticsearch client
	 * @param root dir within the classpath
	 */
	public static void start(Client client, String root) throws Exception {
		logger.info("starting automatic settings/mappings discovery");

		// TODO make it a parameter
		boolean merge = true;
		boolean force = false;

		// create templates
		List<String> templateNames = TemplateFinder.findTemplates(root);
		for (String templateName : templateNames) {
			createTemplate(client, root, templateName, force);
		}

		// create indices
		Collection<String> indexNames = IndexFinder.findIndexNames(root);
		for (String indexName : indexNames) {
			createIndex(client, root, indexName, force);
			updateSettings(client, root, indexName);

			// create types
			List<String> types = TypeFinder.findTypes(root, indexName);
			for (String typeName : types) {
				createMapping(client, root, indexName, typeName, merge);
			}

		}
		logger.info("start done. Rock & roll!");
	}
}
