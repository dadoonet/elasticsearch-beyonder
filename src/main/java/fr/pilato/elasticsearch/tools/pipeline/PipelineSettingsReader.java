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

package fr.pilato.elasticsearch.tools.pipeline;

import java.io.IOException;

import fr.pilato.elasticsearch.tools.SettingsFinder.Defaults;
import fr.pilato.elasticsearch.tools.SettingsReader;

/**
 * Manage elasticsearch pipeline files.
 * 
 * @author hjk181
 */
public class PipelineSettingsReader extends SettingsReader {

    /**
     * Read a pipeline
     * @param root dir within the classpath
     * @param pipeline the id of the pipeline (.json will be appended)
     * @return The pipeline content
     * @throws IOException if the connection with elasticsearch is failing
     */
    public static String readPipeline(String root, String pipeline) throws IOException {
        if (root == null) {
            return readPipeline(pipeline);
        }
        String settingsFile = root + "/" + Defaults.PipelineDir + "/" + pipeline + Defaults.JsonFileExtension;
        return readFileFromClasspath(settingsFile);
    }

    /**
     * Read a pipeline in default classpath dir
     * @param pipeline the id of the pipeline (.json will be appended)
     * @return The pipeline content
     * @throws IOException if the connection with elasticsearch is failing
     */
    public static String readPipeline(String pipeline) throws IOException {
        return readPipeline(Defaults.ConfigDir, pipeline);
    }
}