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

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.util.Collections.singletonList;

public abstract class AbstractBeyonderTest {

    static final Logger logger = LoggerFactory.getLogger(AbstractBeyonderTest.class);

    abstract protected void testBeyonder(String root,
                                         List<String> indices,
                                         List<String> componentTemplates,
                                         List<String> indexTemplates,
                                         List<String> pipelines,
                                         List<String> indexLifecycles) throws Exception;

    @ClassRule
    public static final TemporaryFolder folder = new TemporaryFolder();
    protected static Path rootTmpDir;

    @BeforeClass
    public static void createTmpDir() throws IOException {
        folder.create();
        rootTmpDir = Paths.get(folder.getRoot().toURI());
    }

    @FunctionalInterface
    public interface ThrowingConsumer<E extends Exception> {
        void run() throws E;
    }

    protected void launchAndIgnoreFailure(ThrowingConsumer<Exception> code) {
        try {
            code.run();
        } catch (Exception e) {
            logger.debug("Got an error while calling the cleanup method: {}", e.getMessage());
            logger.trace("StackTrace:", e);
        }
    }

    @Test
    public void testDefaultDir() throws Exception {
        // Default dir es
        testBeyonder(null,
                singletonList("twitter"),
                null, null, null, null);
    }

    @Test
    public void testOneIndexOneType() throws Exception {
        // Single index/single type
        testBeyonder("models/oneindexonetype",
                singletonList("twitter"),
                null, null, null, null);
    }

    @Test
    public void testSettingsAnalyzer() throws Exception {
        // Custom settings (analyzer)
        testBeyonder("models/settingsanalyzer",
                singletonList("twitter"),
                null, null, null, null);
    }

    @Test
    public void testOneIndexNoType() throws Exception {
        // 1 index and no type
        testBeyonder("models/oneindexnotype",
                singletonList("twitter"),
                null, null, null, null);
    }

    @Test
    public void testWrongClasspathDir() throws Exception {
        testBeyonder("models/bad-classpath-7/doesnotexist",
                null, null, null, null, null);
    }
}
