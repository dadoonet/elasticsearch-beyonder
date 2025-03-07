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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

/**
 * This creates a TestContainer Elasticsearch instance
 */
class TestContainerHelper {

    private static final Logger log = LoggerFactory.getLogger(TestContainerHelper.class);

    ElasticsearchContainer elasticsearch;
    private byte[] certAsBytes;

    /**
     * Start the container
     * @param password the password to use
     */
    String startElasticsearch(String version, String password) {
        if (elasticsearch == null) {
            // Start the container. This step might take some time...
            log.info("Starting testcontainers with Elasticsearch [{}].", version);

            elasticsearch = new ElasticsearchContainer(
                    DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch")
                            .withTag(version))
                    .withReuse(true)
                    .withEnv("action.destructive_requires_name", "false")
                    .withPassword(password);
            elasticsearch.start();

            // Try to get the https certificate if exists
            try {
                certAsBytes = elasticsearch.copyFileFromContainer(
                        "/usr/share/elasticsearch/config/certs/http_ca.crt",
                        IOUtils::toByteArray);
                log.debug("Found an https elasticsearch cert for version [{}].", version);
            } catch (Exception e) {
                log.warn("We did not find the https elasticsearch cert for version [{}].", version);
            }
        } else {
            log.info("Testcontainers with Elasticsearch [{}] was previously started", version);
        }

        String url = "https://" + elasticsearch.getHttpHostAddress();

        log.info("Elasticsearch container is now running at {}", url);
        return url;
    }

    public byte[] getCertAsBytes() {
        return certAsBytes;
    }
}
