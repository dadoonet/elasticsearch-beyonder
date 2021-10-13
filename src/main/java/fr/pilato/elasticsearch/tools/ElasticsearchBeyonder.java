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

import fr.pilato.elasticsearch.tools.util.SettingsFinder.Defaults;
import fr.pilato.elasticsearch.tools.util.ResourceList;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

import static fr.pilato.elasticsearch.tools.updaters.ElasticsearchAliasUpdater.manageAliases;
import static fr.pilato.elasticsearch.tools.updaters.ElasticsearchComponentTemplateUpdater.createComponentTemplate;
import static fr.pilato.elasticsearch.tools.updaters.ElasticsearchIndexLifecycleUpdater.createIndexLifecycle;
import static fr.pilato.elasticsearch.tools.updaters.ElasticsearchIndexUpdater.createIndex;
import static fr.pilato.elasticsearch.tools.updaters.ElasticsearchIndexUpdater.updateMapping;
import static fr.pilato.elasticsearch.tools.updaters.ElasticsearchIndexUpdater.updateSettings;
import static fr.pilato.elasticsearch.tools.updaters.ElasticsearchIndexTemplateUpdater.createIndexTemplate;
import static fr.pilato.elasticsearch.tools.updaters.ElasticsearchPipelineUpdater.createPipeline;
import static fr.pilato.elasticsearch.tools.updaters.ElasticsearchTemplateUpdater.createTemplate;

/**
 * By default, indexes are created with their default Elasticsearch settings. You can specify
 * your own settings for your index by putting a /es/indexname/_settings.json in your classpath.
 * <br>
 * So if you create a file named /es/twitter/_settings.json in your src/main/resources folder (for maven lovers),
 * it will be used by the factory to create the twitter index.
 * <pre>
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

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchBeyonder.class);

	/**
	 * Automatically scan classpath and creates indices, types, templates... in default dir.
	 * @param client elasticsearch client
	 * @throws Exception when beyonder can not start
	 */
	public static void start(RestClient client) throws Exception {
		start(client, Defaults.ConfigDir);
	}

	/**
	 * Automatically scan classpath and creates indices, types, templates...
	 * @param client elasticsearch client
	 * @param root dir within the classpath
	 * @throws Exception when beyonder can not start
	 */
	public static void start(RestClient client, String root) throws Exception {
		start(client, root, Defaults.MergeMappings, Defaults.ForceCreation);
	}

	/**
	 * Automatically scan classpath and create indices, mappings, templates, and other settings.
	 * @param client elasticsearch client
	 * @param root dir within the classpath
	 * @param merge whether or not to merge mappings (not used anymore)
	 * @param force whether or not to force creation of indices and templates
	 * @throws Exception when beyonder can not start
	 * @since 6.1
	 */
	@Deprecated
	public static void start(RestClient client, String root, boolean merge, boolean force) throws Exception {
		start(client, root, force);
	}

	/**
	 * Automatically scan classpath and create indices, mappings, templates, and other settings.
	 * @param client elasticsearch client
	 * @param root dir within the classpath
	 * @param force whether or not to force creation of indices and templates
	 * @throws Exception when beyonder can not start
	 */
	public static void start(RestClient client, String root, boolean force) throws Exception {
		logger.info("starting automatic settings/mappings discovery");

		// create legacy templates
		List<String> templateNames = ResourceList.getResourceNames(root, Defaults.TemplatesDir);
		if (templateNames.isEmpty()) {
			// We check for deprecated resources
			templateNames = ResourceList.getResourceNames(root, Defaults.TemplateDir);
			if (!templateNames.isEmpty()) {
				logger.warn("{} dir has been deprecated. Please use {} dir instead.", Defaults.TemplateDir, Defaults.TemplatesDir);
			}
		}
		for (String templateName : templateNames) {
			logger.warn("Legacy Templates are deprecated in Elasticsearch. Switch to Index Templates instead by using {}/{}{}",
					Defaults.IndexTemplatesDir, templateName, Defaults.JsonFileExtension);
			createTemplate(client, root, templateName);
		}

		// create index lifecycles
		List<String> indexLifecycles = ResourceList.getResourceNames(root, Defaults.IndexLifecyclesDir);
		for (String indexLifecycleName : indexLifecycles) {
			createIndexLifecycle(client, root, indexLifecycleName);
		}

		// create component templates
		List<String> componentTemplates = ResourceList.getResourceNames(root, Defaults.ComponentTemplatesDir);
		for (String componentTemplateName : componentTemplates) {
			createComponentTemplate(client, root, componentTemplateName);
		}

		// create index templates
		List<String> indexTemplateNames = ResourceList.getResourceNames(root, Defaults.IndexTemplatesDir);
		for (String indexTemplateName : indexTemplateNames) {
			createIndexTemplate(client, root, indexTemplateName);
		}

		// create pipelines
		List<String> pipelineNames = ResourceList.getResourceNames(root, Defaults.PipelinesDir);
		if (pipelineNames.isEmpty()) {
			// We check for deprecated resources
			pipelineNames = ResourceList.getResourceNames(root, Defaults.PipelineDir);
			if (!pipelineNames.isEmpty()) {
				logger.warn("{} dir has been deprecated. Please use {} dir instead.", Defaults.PipelineDir, Defaults.PipelinesDir);
			}
		}
		for (String pipelineName : pipelineNames) {
			createPipeline(client, root, pipelineName);
		}

		// create indices
		Collection<String> indexNames = ResourceList.findIndexNames(root);
		for (String indexName : indexNames) {
			createIndex(client, root, indexName, force);
			updateSettings(client, root, indexName);
			updateMapping(client, root, indexName);
		}

		// Manage aliases
		manageAliases(client, root);

		logger.info("start done. Rock & roll!");
	}

	/**
	 * Automatically scan classpath and creates indices, types, templates... in default dir.
	 * @param client elasticsearch client
	 * @throws Exception when beyonder can not start
	 * @deprecated You should use now the RestClient implementation
	 * @see #start(RestClient) for the RestClient implementation
	 */
	@Deprecated
	public static void start(Client client) throws Exception {
		start(client, Defaults.ConfigDir);
	}

	/**
	 * Automatically scan classpath and creates indices, types, templates...
	 * @param client elasticsearch client
	 * @param root dir within the classpath
	 * @throws Exception when beyonder can not start
	 * @deprecated You should use now the RestClient implementation
	 * @see #start(RestClient, String) for the RestClient implementation
	 */
	@Deprecated
	public static void start(Client client, String root) throws Exception {
		logger.info("starting automatic settings/mappings discovery");

		boolean force = Defaults.ForceCreation;

		// create legacy templates
		List<String> templateNames = ResourceList.getResourceNames(root, Defaults.TemplatesDir);
		if (templateNames.isEmpty()) {
			// We check for deprecated resources
			templateNames = ResourceList.getResourceNames(root, Defaults.TemplateDir);
			if (!templateNames.isEmpty()) {
				logger.warn("{} dir has been deprecated. Please use {} dir instead.", Defaults.TemplateDir, Defaults.TemplatesDir);
			}
		}
		for (String templateName : templateNames) {
			logger.warn("Legacy Templates are deprecated in Elasticsearch. Switch to Index Templates instead by using {}/{}{}",
					Defaults.IndexTemplatesDir, templateName, Defaults.JsonFileExtension);
			createTemplate(client, root, templateName);
		}

		// create indices
		Collection<String> indexNames = ResourceList.findIndexNames(root);
		for (String indexName : indexNames) {
			createIndex(client, root, indexName, force);
			updateSettings(client, root, indexName);
			updateMapping(client, root, indexName);
		}
		logger.info("start done. Rock & roll!");
	}
}
