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

public class SettingsFinder {

	public static class Defaults {
		/**
		 * Default classpath dir: "elasticsearch"
		 */
		public static String ConfigDir = "elasticsearch";

		public static String JsonFileExtension = ".json";
		public static String IndexSettingsFileName = "_settings";
		public static String UpdateIndexSettingsFileName = "_update_settings";
		public static String UpdateIndexMappingFileName = "_update_mapping";
		@Deprecated
		public static String TemplateDir = "_template";
		public static String TemplatesDir = "_templates";
		public static String IndexTemplatesDir = "_index_templates";
		public static String ComponentTemplatesDir = "_component_templates";
		public static String PipelinesDir = "_pipelines";
		@Deprecated
		public static String PipelineDir = "_pipeline";
		public static String AliasesFile = "_aliases";
		public static String IndexLifecyclesDir = "_index_lifecycles";

		/**
		 * Default setting of whether or not to merge mappings on start.
		 */
		public static boolean MergeMappings = true;

		/**
		 * Default setting of whether or not to force creation of indices and templates on start.
		 */
		public static boolean ForceCreation = false;
	}
}
