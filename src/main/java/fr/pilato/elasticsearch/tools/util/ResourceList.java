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

package fr.pilato.elasticsearch.tools.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * list resources available from the classpath @ *
 * From http://stackoverflow.com/questions/3923129/get-a-list-of-resources-from-classpath-directory
 * http://www.uofr.net/~greg/java/get-resource-listing.html
 * @author Greg Briggs
 */
public class ResourceList {
    private static final Logger logger = LoggerFactory.getLogger(ResourceList.class);
    private static final String[] NO_RESOURCE = {};

    /**
     * List directory contents for a resource folder. Not recursive.
     * This is basically a brute-force implementation.
     * Works for regular files and also JARs.
     *
     * @param root Should end with "/", but not start with one.
     * @return Just the name of each member item, not the full paths.
     * @throws URISyntaxException When a file:// resource can not be converted to URL
     * @throws IOException When a URL can not be decoded
     */
    public static String[] getResources(final String root) throws URISyntaxException, IOException {
        logger.trace("Reading classpath resources from {}", root);
        URL dirURL = ResourceList.class.getClassLoader().getResource(root);
        if (dirURL != null && dirURL.getProtocol().equals("file")) {
            /* A file path: easy enough */
            logger.trace("found a file resource: {}", dirURL);
            String[] resources = new File(dirURL.toURI()).list();
            Arrays.sort(resources);
            return resources;
        }

        if (dirURL == null) {
            /*
             * In case of a jar file, we can't actually find a directory.
             * Have to assume the same jar as clazz.
             */
            String me = ResourceList.class.getName().replace(".", "/")+".class";
            dirURL = ResourceList.class.getClassLoader().getResource(me);
        }

        if (dirURL == null) {
            throw new RuntimeException("can not get resource file " + root);
        }

        if (dirURL.getProtocol().equals("jar")) {
            /* A JAR path */
            logger.trace("found a jar file resource: {}", dirURL);
            String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!")); //strip out only the JAR file
            String prefix = dirURL.getPath().substring(5 + jarPath.length())
                        // remove any ! that a class loader (e.g. from spring boot) could have added
                        .replaceAll("!", "")
                        // remove leading slash that is not part of the JarEntry::getName
                        .substring(1);
            Set<String> result = new HashSet<>(); //avoid duplicates in case it is a subdirectory
            try (JarFile jar = new JarFile(URLDecoder.decode(jarPath, StandardCharsets.UTF_8))) {
                Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
                while(entries.hasMoreElements()) {
                    String name = entries.nextElement().getName();
                    if (name.startsWith(prefix)) { //filter according to the path
                        String entry = name.substring(prefix.length());
                        int checkSubdir = entry.indexOf("/");
                        if (checkSubdir >= 0) {
                            // if it is a subdirectory, we just return the directory name
                            entry = entry.substring(0, checkSubdir);
                        }
                        result.add(entry);
                    }
                }
            }
            String[] resources = result.toArray(new String[result.size()]);
            Arrays.sort(resources);
            return resources;
        }

        // Resource does not exist. We can return an empty list
        logger.trace("did not find any resource. returning empty array");
        return NO_RESOURCE;
    }

    /**
     * Extract the list of available names from a given array of JSON resources
     * @param resources available resource files
     * @return A list of names to use
     */
    public static List<String> extractNamesFromJsonResources(String[] resources) {
        final List<String> names = new ArrayList<>();
        for (String resource : resources) {
            if (!resource.isEmpty()) {
                String withoutIndex = resource.substring(resource.indexOf("/")+1);
                String name = withoutIndex.substring(0, withoutIndex.indexOf(SettingsFinder.Defaults.JsonFileExtension));
                logger.trace(" - found [{}].", name);
                names.add(name);
            }
        }

        return names;
    }

    /**
     * Get the list of resource names (without the .json extension) from a given root and folder
     * @param root Root dir to scan from. Like "/es".
     * @param subdir Subdir name like "_index_templates".
     * @return A list of names we found
     * @throws URISyntaxException When a file:// resource can not be converted to URL
     * @throws IOException When a URL can not be decoded
     */
    public static List<String> getResourceNames(final String root, final String subdir) throws URISyntaxException, IOException {
        String path = root;
        if (root == null) {
            path = SettingsFinder.Defaults.ConfigDir;
        }
        path += "/" + subdir + "/";
        logger.debug("Looking for resources in classpath under [{}].", path);
        return extractNamesFromJsonResources(ResourceList.getResources(path));
    }

    /**
     * Find all indices existing in a given classpath dir
     * @param root dir within the classpath
     * @return a list of indices
     * @throws IOException if connection with elasticsearch is failing
     * @throws URISyntaxException this should not happen
     */
    public static List<String> findIndexNames(final String root) throws IOException, URISyntaxException {
        String path = root;
        if (root == null) {
            path = SettingsFinder.Defaults.ConfigDir;
        }

        logger.debug("Looking for indices in classpath under [{}].", path);

        final List<String> indexNames = new ArrayList<>();
        final Set<String> keys = new HashSet<>();
        String[] resources = ResourceList.getResources(path + "/"); // "es/" or "a/b/c/"
        for (String resource : resources) {
            if (!resource.isEmpty()) {
                logger.trace(" - resource [{}].", resource);
                String key;
                if (resource.contains("/")) {
                    key = resource.substring(0, resource.indexOf("/"));
                } else {
                    key = resource;
                }
                if (!key.equals(SettingsFinder.Defaults.IndexTemplatesDir) &&
                        !key.equals(SettingsFinder.Defaults.ComponentTemplatesDir) &&
                        !key.equals(SettingsFinder.Defaults.TemplateDir) &&
                        !key.equals(SettingsFinder.Defaults.TemplatesDir) &&
                        !key.equals(SettingsFinder.Defaults.PipelineDir) &&
                        !key.equals(SettingsFinder.Defaults.PipelinesDir) &&
                        !keys.contains(key)) {
                    logger.trace(" - found [{}].", key);
                    keys.add(key);
                    indexNames.add(key);
                }
            }
        }

        return indexNames;
    }
}
