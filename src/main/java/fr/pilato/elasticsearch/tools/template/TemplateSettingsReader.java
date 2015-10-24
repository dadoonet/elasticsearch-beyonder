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

package fr.pilato.elasticsearch.tools.template;

import fr.pilato.elasticsearch.tools.SettingsFinder.Defaults;
import fr.pilato.elasticsearch.tools.SettingsReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

import static fr.pilato.elasticsearch.tools.SettingsFinder.fromClasspath;

/**
 * Manage elasticsearch template files
 * @author David Pilato
 */
public class TemplateSettingsReader extends SettingsReader {

	private static final Logger logger = LogManager.getLogger(TemplateSettingsReader.class);

	/**
	 * Read a template
	 * @param root dir within the classpath
	 * @param template template name (.json will be appended)
	 */
	public static String readTemplate(String root, String template) throws IOException {
		if (root == null) {
			return readTemplate(template);
		}
		String settingsFile = root + "/" + Defaults.TemplateDir + "/" + template + Defaults.JsonFileExtension;
		return readFileFromClasspath(settingsFile);
	}

	/**
	 * Read a template in default classpath dir
	 * @param template template name (.json will be appended)
	 */
	public static String readTemplate(String template) throws IOException {
		return readTemplate(fromClasspath(Defaults.ConfigDir), template);
	}
}
