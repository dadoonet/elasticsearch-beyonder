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

package fr.pilato.elasticsearch.tools.indextemplate;

import fr.pilato.elasticsearch.tools.SettingsFinder.Defaults;
import fr.pilato.elasticsearch.tools.SettingsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Manage elasticsearch index template files in _index_templates dir
 * and component template files in _component_templates dir.
 * @author David Pilato
 */
public class IndexTemplateReader extends SettingsReader {

	private static final Logger logger = LoggerFactory.getLogger(IndexTemplateReader.class);

	/**
	 * Read an index template
	 * @param root dir within the classpath
	 * @param template index template name (.json will be appended)
	 * @return The index template content
	 * @throws IOException if we can not read the file
	 */
	public static String readIndexTemplate(String root, String template) throws IOException {
		if (root == null) {
			return readIndexTemplate(template);
		}
		String settingsFile = root + "/" + Defaults.IndexTemplatesDir + "/" + template + Defaults.JsonFileExtension;
		return readFileFromClasspath(settingsFile);
	}

	/**
	 * Read an index template in default classpath dir
	 * @param template index template name (.json will be appended)
	 * @return The index template content
	 * @throws IOException if we can not read the file
	 */
	public static String readIndexTemplate(String template) throws IOException {
		return readIndexTemplate(Defaults.ConfigDir, template);
	}
}
