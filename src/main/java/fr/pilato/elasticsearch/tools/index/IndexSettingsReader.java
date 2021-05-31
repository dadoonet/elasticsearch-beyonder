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

import fr.pilato.elasticsearch.tools.SettingsFinder.Defaults;
import fr.pilato.elasticsearch.tools.SettingsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Manage elasticsearch index setting files
 * @author David Pilato
 */
public class IndexSettingsReader extends SettingsReader {

	private static final Logger logger = LoggerFactory.getLogger(IndexSettingsReader.class);

	/**
	 * Read index settings
	 * @param root dir within the classpath
	 * @param index index name
	 * @param jsonFile json file to read
	 * @return Settings
	 */
	public static String readFile(String root, String index, String jsonFile) {
		logger.trace("Reading [{}] for [{}] in [{}]...", jsonFile, index, root);
		String settingsFile = root + "/" + index + "/" + jsonFile;
		return readFileFromClasspath(settingsFile);
	}

	/**
	 * Read index settings
	 * @param root dir within the classpath
	 * @param index index name
     * @return Settings
     * @throws IOException if connection with elasticsearch is failing
     */
	public static String readSettings(String root, String index) throws IOException {
		if (root == null) {
			return readSettings(index);
		}
		return readFile(root, index, Defaults.IndexSettingsFileName);
	}

	/**
	 * Read index settings in default classpath dir
	 * @param index index name
     * @return Settings
     * @throws IOException if connection with elasticsearch is failing
	 */
	public static String readSettings(String index) throws IOException {
		return readSettings(Defaults.ConfigDir, index);
	}

	/**
	 * Read index settings
	 * @param root dir within the classpath
	 * @param index index name
     * @return Update Settings
	 */
	public static String readUpdateSettings(String root, String index) {
		return readFile(root, index, Defaults.UpdateIndexSettingsFileName);
	}

	/**
	 * Read index settings in default classpath dir
	 * @param index index name
     * @return Update Settings
     */
	public static String readUpdateSettings(String index) {
		return readUpdateSettings(Defaults.ConfigDir, index);
	}

	/**
	 * Read index mapping
	 * @param root dir within the classpath
	 * @param index index name
     * @return Update Mapping
	 */
	public static String readUpdateMapping(String root, String index) {
		return readFile(root, index, Defaults.UpdateIndexMappingFileName);
	}

	/**
	 * Read index mapping in default classpath dir
	 * @param index index name
     * @return Update Mapping
     */
	public static String readUpdateMapping(String index) {
		return readUpdateMapping(Defaults.ConfigDir, index);
	}
}
