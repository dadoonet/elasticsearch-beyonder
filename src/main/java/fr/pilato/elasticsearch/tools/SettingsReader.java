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

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

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
			content = IOUtils.toString(asStream, "UTF-8");
		} catch (IOException e) {
			logger.warn("Can not read [{}].", file);
		}

		return StringSubstitutor.replace(content, System.getenv());
	}

	public static String getJsonContent(String root, String subdir, String name) throws IOException {
		String path = root;
		if (root == null) {
			path = SettingsFinder.Defaults.ConfigDir;
		}
		path += "/" + subdir + "/" + name + SettingsFinder.Defaults.JsonFileExtension;
		logger.debug("Reading file [{}] from the classpath.", path);
		return readFileFromClasspath(path);
	}
}
