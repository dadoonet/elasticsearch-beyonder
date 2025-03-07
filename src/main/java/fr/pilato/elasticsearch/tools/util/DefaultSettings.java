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

package fr.pilato.elasticsearch.tools.util;

/**
 * Settings finder
 */
public class DefaultSettings {
	private DefaultSettings() {
		// empty
	}

	/**
	 * Default classpath dir: "elasticsearch"
	 */
	public static final String ConfigDir = "elasticsearch";

	/**
	 * Json file extension: ".json"
	 */
	public static final String JsonFileExtension = ".json";

	/**
	 * NdJson file extension: ".ndjson"
	 */
	public static final String NdJsonFileExtension = ".ndjson";

	/**
	 * Default index settings file name: "_settings.json"
	 */
	public static final String IndexSettingsFileName = "_settings.json";

	/**
	 * Default index update settings file name: "_update_settings.json"
	 */
	public static final String UpdateIndexSettingsFileName = "_update_settings.json";

	/**
	 * Default index update mapping file name: "_update_mapping"
	 */
	public static final String UpdateIndexMappingFileName = "_update_mapping.json";

	/**
	 * Default index templates directory: "_index_templates"
	 */
	public static final String IndexTemplatesDir = "_index_templates";

	/**
	 * Default component templates directory: "_component_templates"
	 */
	public static final String ComponentTemplatesDir = "_component_templates";

	/**
	 * Default ingest pipelines directory: "_pipelines"
	 */
	public static final String PipelinesDir = "_pipelines";

	/**
	 * Default aliases file : "_aliases.json"
	 */
	public static final String AliasesFile = "_aliases.json";

	/**
	 * Default index lifecycles directory: "_index_lifecycles"
	 */
	public static final String IndexLifecyclesDir = "_index_lifecycles";

	/**
	 * Default data directory: "_data"
	 */
	public static final String DataDir = "_data";

	/**
	 * Default setting of whether to force creation of indices and templates on start.
	 */
	public static final boolean ForceCreation = false;
}
