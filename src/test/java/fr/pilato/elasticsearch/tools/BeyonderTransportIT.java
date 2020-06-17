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

package fr.pilato.elasticsearch.tools;

import static fr.pilato.elasticsearch.tools.index.IndexElasticsearchUpdater.isIndexExist;
import static fr.pilato.elasticsearch.tools.template.TemplateElasticsearchUpdater.isTemplateExist;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeNoException;
import static org.junit.Assume.assumeTrue;

import java.util.List;

import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BeyonderTransportIT extends AbstractBeyonderTest {

    private static final Logger logger = LoggerFactory.getLogger(BeyonderTransportIT.class);

    @BeforeClass
    public static void setTestBehavior() {
        try {
        	transportClient.admin().cluster().prepareNodesInfo().get();
        } catch (NoNodeAvailableException e) {
            assumeNoException(e);
        }
    }

    @Before
    public void cleanCluster() {
        try {
        	transportClient.admin().indices().prepareDelete("_all").get();
        } catch (NoNodeAvailableException e) {
            assumeNoException(e);
        }
    }

    @Override
    @Test
    public void testDefaultDir() {
        assumeTrue("We skip the default dir test for transport client as the index " +
                "settings format is not compatible with rest client (type needs to " +
                "be provided)", false);
    }

    @Override
	protected void testBeyonder(String root,
                                List<String> indices,
                                List<String> templates) throws Exception {
        String newRoot = "transport/" + root;
        logger.info("--> scanning: [{}]", newRoot);
        ElasticsearchBeyonder.start(client, newRoot);

        // We can now check if we have the templates created
        if (templates != null) {
            boolean allExists = true;

            for (String template : templates) {
                if (!isTemplateExist(client, template)) {
                    allExists = false;
                }
            }
            assertThat(allExists, is(true));
        }

        // We can now check if we have the indices created
        if (indices != null) {
            boolean allExists = true;

            for (String index : indices) {
                if (!isIndexExist(client, index)) {
                    allExists = false;
                }
            }
            assertThat(allExists, is(true));
        }
    }
}
