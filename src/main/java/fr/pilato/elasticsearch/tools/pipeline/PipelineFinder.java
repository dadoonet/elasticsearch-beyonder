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

import fr.pilato.elasticsearch.tools.ResourceList;
import fr.pilato.elasticsearch.tools.SettingsFinder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Find ingest pipelines on the classpath.
 * 
 * @author hjk181
 *
 */
public class PipelineFinder extends SettingsFinder {

    /**
     * Find all pipelines ("elasticsearch/_pipeline/")
     * 
     * @param root dir within the classpath
     * @return a list of pipelines
     * @throws IOException if connection with elasticsearch is failing
     * @throws URISyntaxException this should not happen
     */
    public static List<String> findPipelines(String root) throws IOException, URISyntaxException {
        return ResourceList.getResourceNames(root, Defaults.PipelineDir);
    }
}
