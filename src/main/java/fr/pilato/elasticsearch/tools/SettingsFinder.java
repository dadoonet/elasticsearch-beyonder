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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

import static java.nio.file.FileVisitResult.CONTINUE;

public class SettingsFinder {

	private static final Logger logger = LoggerFactory.getLogger(SettingsFinder.class);

	public static class Defaults {
		/**
		 * Default classpath dir: "elasticsearch"
		 */
		public static String ConfigDir = "elasticsearch";

		public static String JsonFileExtension = ".json";
		public static String IndexSettingsFileName = "_settings.json";
		public static String UpdateIndexSettingsFileName = "_update_settings.json";
		public static String TemplateDir = "_template";
		public static String PipelineDir = "_pipeline";

		/**
		 * Default setting of whether or not to merge mappings on start.
		 */
		public static boolean MergeMappings = true;

		/**
		 * Default setting of whether or not to force creation of indices and templates on start.
		 */
		public static boolean ForceCreation = false;
	}

	/**
	 * Find all types within an index
	 * @param root dir within the classpath
	 * @param subdir subdir name
     * @return A list of found JSON files
     * @deprecated Sounds like it's not used. We will remove it
     * @throws IOException if something goes wrong
	 */
	@Deprecated
	protected static ArrayList<String> findJsonFiles(Path root, String subdir) throws IOException {
		logger.debug("Looking for json files in classpath under [{}/{}].", root, subdir);

		final ArrayList<String> jsonFiles = new ArrayList<>();
		final Path indexDir = root.resolve(subdir);
		if (Files.exists(indexDir)) {
			Files.walkFileTree(indexDir, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					// We have now files. They could be type, settings, templates...
					String jsonFile = indexDir.relativize(file).toString();

					if (jsonFile.equals(Defaults.IndexSettingsFileName) ||
							jsonFile.equals(Defaults.UpdateIndexSettingsFileName)) {
						logger.trace("ignoring: [{}]", jsonFile);
						return CONTINUE;
					}
					jsonFile = jsonFile.substring(0, jsonFile.lastIndexOf(Defaults.JsonFileExtension));

					jsonFiles.add(jsonFile);
					logger.trace("json found: [{}]", jsonFile);
					return CONTINUE;
				}
			});
		} else {
			logger.trace("[{}] does not exist in [{}].", subdir, root);
		}

		return jsonFiles;
	}
}
