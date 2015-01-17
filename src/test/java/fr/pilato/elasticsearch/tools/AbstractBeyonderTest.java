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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;

public abstract class AbstractBeyonderTest {

    protected static final Logger logger = LogManager.getLogger(AbstractBeyonderTest.class);

    abstract protected void testBeyonder(String root,
                                List<String> indices,
                                List<List<String>> types,
                                List<String> templates) throws Exception;

    @Test
    public void testDefaultDir() throws Exception {
        // Default dir es
        testBeyonder(null,
                asList("twitter"),
                asList(asList("tweet")),
                null);
    }

    @Test
    public void testOneIndexOneType() throws Exception {
        // Single index/single type
        testBeyonder("models/oneindexonetype",
                asList("twitter"),
                asList(asList("tweet")),
                null);
    }

    @Test
    public void testTwoIndicesTwoTypesOneType() throws Exception {
        // 2 indices: 2 types and 1 type
        testBeyonder("models/twoindicestwotypesonetype",
                asList("rss", "twitter"),
                asList(asList("doc1", "doc2"), asList("tweet")),
                null);
    }

    @Test
    public void testSettingsAnalyzer() throws Exception {
        // Custom settings (analyzer)
        testBeyonder("models/settingsanalyzer",
                asList("twitter"),
                asList(asList("tweet")),
                null);
    }

    @Test
    public void testOneIndexNoType() throws Exception {
        // 1 index and no type
        testBeyonder("models/oneindexnotype",
                asList("twitter"),
                asList((List<String>) null),
                null);
    }

    @Test
    public void testTemplate() throws Exception {
        // 1 template
        testBeyonder("models/template",
                null,
                null,
                asList("twitter_template"));
    }

    @Test
    public void testUpdateSettings() throws Exception {
        // 1 _update_settings
        testBeyonder("models/update-settings/step1",
                asList("twitter"),
                asList(asList("tweet")),
                null);
        testBeyonder("models/update-settings/step2",
                asList("twitter"),
                asList((List<String>) null),
                null);
    }

    @Test
    public void testWrongClasspathDir() throws Exception {
        testBeyonder("models/bad-classpath-7/doesnotexist",
                null,
                null,
                null);
    }
}
