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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.pilato.elasticsearch.tools.ResourceList;
import fr.pilato.elasticsearch.tools.SettingsFinder;
import fr.pilato.elasticsearch.tools.template.TemplateFinder;

/**
 * Findes ingest pipelines on the classpath.
 * 
 * @author hjk181
 *
 */
public class PipelineFinder extends SettingsFinder {

    private static final Logger logger = LoggerFactory.getLogger(TemplateFinder.class);

    /**
     * Find all pipelines in default classpath dir
     * 
     * @return a list of pipelines
     * @throws IOException if connection with elasticsearch is failing
     * @throws URISyntaxException this should not happen
     */
    public static List<String> findPipelines() throws IOException, URISyntaxException {
        return findPipelines(Defaults.ConfigDir);
    }

    /**
     * Find all pipelines
     * 
     * @param root dir within the classpath
     * @return a list of pipelines
     * @throws IOException if connection with elasticsearch is failing
     * @throws URISyntaxException this should not happen
     */
    public static List<String> findPipelines(String root) throws IOException, URISyntaxException {
        if (root == null) {
            return findPipelines();
        }

        logger.debug("Looking for pipelines in classpath under [{}].", root);

        final List<String> pipelineNames = new ArrayList<>();
        String[] resources = ResourceList.getResources(root + "/" + Defaults.PipelineDir + "/"); // "es/_pipeline/"
        for (String resource : resources) {
            if (!resource.isEmpty()) {
                String withoutIndex = resource.substring(resource.indexOf("/") + 1);
                String pipeline = withoutIndex.substring(0, withoutIndex.indexOf(Defaults.JsonFileExtension));
                logger.trace(" - found [{}].", pipeline);
                pipelineNames.add(pipeline);
            }
        }

        return pipelineNames;
    }
}