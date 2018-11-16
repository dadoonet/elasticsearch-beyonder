/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

package fr.pilato.elasticsearch.tools.type;

import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage elasticsearch types (mappings)
 * @author David Pilato
 */
public class TypeElasticsearchUpdater {

    private static final Logger logger = LoggerFactory.getLogger(TypeElasticsearchUpdater.class);


    /**
     * Create a new type in Elasticsearch
     * @param client Elasticsearch client
     * @param index Index name
     * @param type Type name
     * @param merge Try to merge mapping if type already exists
     * @throws Exception if the elasticsearch call is failing
     */
    @Deprecated
    public static void createMapping(Client client, String index, String type, boolean merge) throws Exception {
        String mapping = TypeSettingsReader.readMapping(index, type);
        createMappingWithJson(client, index, type, mapping, merge);
    }

    /**
     * Create a new type in Elasticsearch
     * @param client Elasticsearch client
     * @param root dir within the classpath
     * @param index Index name
     * @param type Type name
     * @param merge Try to merge mapping if type already exists
     * @throws Exception if the elasticsearch call is failing
     */
    @Deprecated
    public static void createMapping(Client client, String root, String index, String type, boolean merge)
            throws Exception {
        String mapping = TypeSettingsReader.readMapping(root, index, type);
        createMappingWithJson(client, index, type, mapping, merge);
    }

    /**
     * Create a new type in Elasticsearch
     * @param client Elasticsearch client
     * @param index Index name
     * @param type Type name
     * @param mapping Mapping if any, null if no specific mapping
     * @param merge Try to merge mapping if type already exists
     * @throws Exception if the elasticsearch call is failing
     */
    @Deprecated
    public static void createMappingWithJson(Client client, String index, String type, String mapping, boolean merge)
            throws Exception {
        boolean mappingExist = isTypeExist(client, index);
        if (merge || !mappingExist) {
            if (mappingExist) {
                logger.debug("Updating type [{}]/[{}].", index, type);
            } else {
                logger.debug("Type [{}]/[{}] doesn't exist. Creating it.", index, type);
            }
            createTypeWithMappingInElasticsearch(client, index, type, mapping);
        } else {
            logger.debug("Type [{}/{}] already exists and merge is not set.", index, type);
        }

        if (mappingExist) {
            logger.debug("Type definition for [{}]/[{}] succesfully merged.", index, type);
        } else {
            logger.debug("Type definition for [{}]/[{}] succesfully created.", index, type);
        }
    }

    /**
     * Check if a type already exists
     * @param client Elasticsearch client
     * @param index Index name
     * @return true if a mapping already exists
     */
    @Deprecated
    public static boolean isTypeExist(Client client, String index) {
        return !client.admin().indices().prepareGetMappings(index).get().getMappings().isEmpty();
    }

    /**
     * Create a new type in Elasticsearch
     * @param client Elasticsearch client
     * @param index Index name
     * @param type Type name
     * @param mapping Mapping if any, null if no specific mapping
     * @throws Exception if the elasticsearch call is failing
     */
    @Deprecated
    private static void createTypeWithMappingInElasticsearch(Client client, String index, String type, String mapping)
            throws Exception {
        logger.trace("createType([{}/{}])", index, type);

        assert client != null;
        assert index != null;
        assert type != null;

        if (mapping != null) {
            // Create type and mapping
            AcknowledgedResponse response = client.admin().indices().preparePutMapping(index)
                    .setType(type)
                    .setSource(mapping, XContentType.JSON).get();
            if (!response.isAcknowledged()) {
                logger.warn("Could not create type [{}/{}]", index, type);
                throw new Exception("Could not create type ["+index+"/"+type+"].");
            }
        } else {
            logger.trace("no content given for mapping. Ignoring type [{}/{}] creation.", index, type);
        }

        logger.trace("/createType([{}/{}])", index, type);
    }

    /**
     * Create a new type in Elasticsearch
     * @param client Elasticsearch client
     * @param index Index name
     * @param type Type name
     * @param merge Try to merge mapping if type already exists
     * @throws Exception if the elasticsearch call is failing
     */
    public static void createMapping(RestClient client, String index, String type, boolean merge) throws Exception {
        String mapping = TypeSettingsReader.readMapping(index, type);
        createMappingWithJson(client, index, type, mapping, merge);
    }

    /**
     * Create a new type in Elasticsearch
     * @param client Elasticsearch client
     * @param root dir within the classpath
     * @param index Index name
     * @param type Type name
     * @param merge Try to merge mapping if type already exists
     * @throws Exception if the elasticsearch call is failing
     */
    public static void createMapping(RestClient client, String root, String index, String type, boolean merge)
            throws Exception {
        String mapping = TypeSettingsReader.readMapping(root, index, type);
        createMappingWithJson(client, index, type, mapping, merge);
    }

    /**
     * Create a new type in Elasticsearch
     * @param client Elasticsearch client
     * @param index Index name
     * @param type Type name
     * @param mapping Mapping if any, null if no specific mapping
     * @param merge Try to merge mapping if type already exists
     * @throws Exception if the elasticsearch call is failing
     */
    public static void createMappingWithJson(RestClient client, String index, String type, String mapping, boolean merge)
            throws Exception {
        boolean mappingExist = isTypeExist(client, index, type);
        if (merge || !mappingExist) {
            if (mappingExist) {
                logger.debug("Updating type [{}]/[{}].", index, type);
            } else {
                logger.debug("Type [{}]/[{}] doesn't exist. Creating it.", index, type);
            }
            createTypeWithMappingInElasticsearch(client, index, type, mapping);
        } else {
            logger.debug("Type [{}/{}] already exists and merge is not set.", index, type);
        }

        if (mappingExist) {
            logger.debug("Type definition for [{}]/[{}] succesfully merged.", index, type);
        } else {
            logger.debug("Type definition for [{}]/[{}] succesfully created.", index, type);
        }
    }

    /**
     * Check if a type already exists
     * @param client Elasticsearch client
     * @param index Index name
     * @param type Type name
     * @return true if type already exists
     * @throws Exception if the elasticsearch call is failing
     */
    public static boolean isTypeExist(RestClient client, String index, String type) throws Exception {
        try {
            client.performRequest(new Request("GET", "/" + index + "/_mapping/" + type));
            return true;
        } catch (ResponseException e) {
            if (e.getResponse().getStatusLine().getStatusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    /**
     * Create a new type in Elasticsearch
     * @param client Elasticsearch client
     * @param index Index name
     * @param type Type name
     * @param mapping Mapping if any, null if no specific mapping
     * @throws Exception if the elasticsearch call is failing
     */
    private static void createTypeWithMappingInElasticsearch(RestClient client, String index, String type, String mapping)
            throws Exception {
        logger.trace("createType([{}/{}])", index, type);

        assert client != null;
        assert index != null;
        assert type != null;

        if (mapping != null) {
            // Create type and mapping
            Request request = new Request("PUT", "/" + index + "/_mapping/" + type);
            request.setJsonEntity(mapping);
            Response response = client.performRequest(request);
            if (response.getStatusLine().getStatusCode() != 200) {
                logger.warn("Could not create type [{}/{}]", index, type);
                throw new Exception("Could not create type ["+index+"/"+type+"].");
            }
        } else {
            logger.trace("no content given for mapping. Ignoring type [{}/{}] creation.", index, type);
        }

        logger.trace("/createType([{}/{}])", index, type);
    }
}
