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

import fr.pilato.elasticsearch.tools.util.ResourceList;
import fr.pilato.elasticsearch.tools.util.SettingsReader;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

import static fr.pilato.elasticsearch.tools.util.ResourceList.replaceIndexName;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ResourceListTest {

    @Test
    public void testClasspath() throws IOException, URISyntaxException {
        final String[] list = ResourceList.getResources("\\.json");
        for(final String name : list) {
            System.out.println(name);
            String file = SettingsReader.readFileFromClasspath(name);
            System.out.println(file);
        }
    }

    @Test
    public void testPattern() {
        String root = "es";
        String filename = "/Users/dpilato/Documents/Elasticsearch/dev/spring-elasticsearch/target/test-classes/es/twitter/_settings.json";
        Pattern pattern = Pattern.compile(".*/" + root + "/.*");
        assertThat(pattern.matcher(filename).matches(), is(true));
    }

    @Test
    public void testIndexNames() {
        // We test simple index names
        assertThat(replaceIndexName("foo"), is("foo"));
        assertThat(replaceIndexName("foo-001"), is("foo-001"));
        assertThat(replaceIndexName("000001-foo"), is("000001-foo"));

        // We test rollover indices
        assertThat(replaceIndexName("my-index-000001"), is("my-index-*"));
        assertThat(replaceIndexName("my-index-001234"), is("my-index-*"));

        // We test date maths
        assertThat(replaceIndexName("<my-index-{now/d}>"), is("my-index-*"));
        assertThat(replaceIndexName("<my-index-{now/d}-000001>"), is("my-index-*-*"));

        // The same but with URL encoded characters
        assertThat(replaceIndexName("%3Cmy-index-%7Bnow%2Fd%7D%3E"), is("my-index-*"));
        assertThat(replaceIndexName("%3Cmy-index-%7Bnow%2Fd%7D-000001%3E"), is("my-index-*-*"));
    }
}
