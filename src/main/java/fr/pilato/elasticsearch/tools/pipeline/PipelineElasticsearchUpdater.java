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

import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles ingest pipeline creation.
 * 
 * @author hjk181
 *
 */
public class PipelineElasticsearchUpdater {

    private static final Logger logger = LoggerFactory.getLogger(PipelineElasticsearchUpdater.class);

    /**
     * Create a pipeline in Elasticsearch.
     * 
     * @param client Elasticsearch client
     * @param root dir within the classpath
     * @param pipeline the id of the pipeline
     * @param force set it to true if you want to force cleaning pipeline before adding it
     * @throws Exception if something goes wrong
     */
    public static void createPipeline(RestClient client, String root, String pipeline, boolean force) throws Exception {
        String json = PipelineSettingsReader.readPipeline(root, pipeline);
        createPipelineWithJson(client, pipeline, json, force);
    }

    /**
     * Create a pipeline in Elasticsearch. Read content from default classpath dir.
     * 
     * @param client Elasticsearch client
     * @param pipeline the id of the pipeline
     * @param force set it to true if you want to force cleaning pipeline before adding it
     * @throws Exception if something goes wrong
     */
    public static void createPipeline(RestClient client, String pipeline, boolean force) throws Exception {
        String json = PipelineSettingsReader.readPipeline(pipeline);
        createPipelineWithJson(client, pipeline, json, force);
    }

    /**
     * Create a new pipeline in Elasticsearch
     * 
     * @param client Elasticsearch client
     * @param pipeline the id of the pipeline
     * @param json JSon content for the pipeline
     * @param force set it to true if you want to force cleaning pipeline before adding it
     * @throws Exception if something goes wrong
     */
    public static void createPipelineWithJson(RestClient client, String pipeline, String json, boolean force) throws Exception {
        if (isPipelineExist(client, pipeline)) {
            if (force) {
                logger.debug("Pipeline [{}] already exists. Force is set. Overriding it.", pipeline);
                createPipelineWithJsonInElasticsearch(client, pipeline, json);
            }
            else {
                logger.debug("Pipeline [{}] already exists.", pipeline);
            }
        }

        if (!isPipelineExist(client, pipeline)) {
            logger.debug("Pipeline [{}] doesn't exist. Creating it.", pipeline);
            createPipelineWithJsonInElasticsearch(client, pipeline, json);
        }
    }

    /**
     * Create a new pipeline in Elasticsearch.
     * 
     * @param client Elasticsearch client
     * @param pipeline the id of the pipeline
     * @param json JSon content for the pipeline
     * @throws Exception if something goes wrong
     */
    private static void createPipelineWithJsonInElasticsearch(RestClient client, String pipeline, String json) throws Exception {
        logger.trace("createPipeline([{}])", pipeline);

        assert client != null;
        assert pipeline != null;

        Request request = new Request("PUT", "/_ingest/pipeline/" + pipeline);
        request.setJsonEntity(json);
        Response response = client.performRequest(request);

        if (response.getStatusLine().getStatusCode() != 200) {
            logger.warn("Could not create pipeline [{}]", pipeline);
            throw new Exception("Could not create pipeline [" + pipeline + "].");
        }

        logger.trace("/createPipeline([{}])", pipeline);
    }

    /**
     * Check if a pipeline exists
     * 
     * @param client Elasticsearch client
     * @param pipeline the id of the pipeline
     * @return true if the pipeline exists
     * @throws IOException if something goes wrong
     */
    public static boolean isPipelineExist(RestClient client, String pipeline) throws IOException {
        try {
            Response response = client.performRequest(new Request("GET", "/_ingest/pipeline/" + pipeline));
            return response.getEntity() != null;
        }
        catch (ResponseException e) {
            if (404 != e.getResponse().getStatusLine().getStatusCode()) {
                throw e;
            }
        }
        return false;
    }
}
