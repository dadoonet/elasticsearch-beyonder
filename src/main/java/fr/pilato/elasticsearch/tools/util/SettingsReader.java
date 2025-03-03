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

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class SettingsReader {

	private static final Logger logger = LoggerFactory.getLogger(SettingsReader.class);

	/**
	 * Read a file content from the classpath
	 * @param file filename
	 * @return The file content
	 */
	public static String readFileFromClasspath(String file) {
		logger.trace("Reading file [{}]...", file);
		String content = null;

		try (InputStream asStream = SettingsReader.class.getClassLoader().getResourceAsStream(file)) {
			if (asStream == null) {
				logger.trace("Can not find [{}] in class loader.", file);
				return null;
			}
			content = IOUtils.toString(asStream, StandardCharsets.UTF_8);
		} catch (IOException e) {
			logger.warn("Can not read [{}].", file);
		}

		return content;
	}

	/**
	 * This method will read a file from the classpath and replace variables with environment variables
	 * @param root		The root directory
	 * @param subdir	The subdirectory
	 * @param name		The resource name without the .json extension
	 * @return The content of the file
	 */
	public static String getJsonContent(String root, String subdir, String name) {
		String content = getFileContent(root, subdir, name + SettingsFinder.Defaults.JsonFileExtension);
		return StringSubstitutor.replace(content, System.getenv());
	}

	public static String getFileContent(String root, String subdir, String name) {
		String path = root;
		if (root == null) {
			path = SettingsFinder.Defaults.ConfigDir;
		}
		if (subdir != null) {
			path += "/" + subdir;
		}
		path += "/" + name;
		logger.debug("Reading file [{}] from the classpath.", path);
		return readFileFromClasspath(path);
	}
}
