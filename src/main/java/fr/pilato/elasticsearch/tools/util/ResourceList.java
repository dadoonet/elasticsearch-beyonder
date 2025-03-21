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
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * list resources available from the classpath @ *
 * From <a href="http://stackoverflow.com/questions/3923129/get-a-list-of-resources-from-classpath-directory">stackoverflow</a>
 * <a href="http://www.uofr.net/~greg/java/get-resource-listing.html">Greg Briggs' Technical Articles</a>
 * @author Greg Briggs
 */
public class ResourceList {
    private static final Logger logger = LoggerFactory.getLogger(ResourceList.class);
    private static final String[] NO_RESOURCE = {};

    private ResourceList() {
        // empty
    }

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

            if (resources == null) {
                // We return an empty array
                return NO_RESOURCE;
            }

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
            String[] resources = result.toArray(new String[0]);
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
                String name = withoutIndex.substring(0, withoutIndex.indexOf(DefaultSettings.JsonFileExtension));
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
            path = DefaultSettings.ConfigDir;
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
            path = DefaultSettings.ConfigDir;
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
                if (!key.equals(DefaultSettings.IndexTemplatesDir) &&
                        !key.equals(DefaultSettings.ComponentTemplatesDir) &&
                        !key.equals(DefaultSettings.PipelinesDir) &&
                        !key.equals(DefaultSettings.AliasesFile) &&
                        !key.equals(DefaultSettings.IndexLifecyclesDir) &&
                        !key.equals(DefaultSettings.DataDir) &&
                        !keys.contains(key)) {
                    logger.trace(" - found [{}].", key);
                    keys.add(key);
                    indexNames.add(key);
                }
            }
        }

        return indexNames;
    }

    /**
     * Find all bulk files (*.ndjson) existing in a given classpath dir for a given index under the _data subdir
     *
     * @param root  dir within the classpath
     * @param index index name
     * @return a set of bulk files (*.ndjson)
     * @throws IOException        if we can't read the classpath or the filesystem
     * @throws URISyntaxException this should not happen
     */
    public static Collection<String> findBulkFiles(final String root, final String index) throws IOException, URISyntaxException {
        return findFilesByExtension(root, index, DefaultSettings.NdJsonFileExtension);
    }

    /**
     * Find all json files (*.json) existing in a given classpath dir for a given index under the _data subdir
     *
     * @param root  dir within the classpath
     * @param index index name
     * @return a set of json files (*.json)
     * @throws IOException        if we can't read the classpath or the filesystem
     * @throws URISyntaxException this should not happen
     */
    public static Collection<String> findJsonFiles(final String root, final String index) throws IOException, URISyntaxException {
        return findFilesByExtension(root, index, DefaultSettings.JsonFileExtension);
    }

    /**
     * Find all files matching a given extension and existing in a given classpath dir for a given index under the _data subdir
     *
     * @param root  dir within the classpath
     * @param index index name
     * @param extension the extension to look for like json or ndjson
     * @return a set of files
     * @throws IOException        if we can't read the classpath or the filesystem
     * @throws URISyntaxException this should not happen
     */
    private static Collection<String> findFilesByExtension(final String root, final String index, final String extension) throws IOException, URISyntaxException {
        String path = root;
        String indexName = index;
        if (path == null) {
            path = DefaultSettings.ConfigDir;
        }
        if (indexName == null) {
            indexName = DefaultSettings.DataDir;
        } else {
            indexName = indexName + "/" + DefaultSettings.DataDir;
        }

        logger.debug("Looking for [{}] files in classpath under [{}/{}].", extension, path, indexName);

        final Set<String> filenames = new HashSet<>();
        String[] resources = ResourceList.getResources(path + "/" + indexName); // "es/" or "a/b/c/"
        for (String resource : resources) {
            if (!resource.isEmpty()) {
                logger.trace(" - resource [{}].", resource);
                String key;
                if (resource.contains("/")) {
                    key = resource.substring(0, resource.indexOf("/"));
                } else {
                    key = resource;
                }
                if (key.endsWith(extension) && !filenames.contains(key)) {
                    logger.trace(" - found [{}].", key);
                    filenames.add(key);
                }
            }
        }

        // Sort the collection before returning it
        List<String> sortedFilenames = new ArrayList<>(filenames);
        Collections.sort(sortedFilenames);

        return sortedFilenames;
    }

    /**
     * Replace index name from a form of {@code "<my-index-{now/d}-000001>"} or
     * {@code "%3Cmy-index-%7Bnow%2Fd%7D-000001%3E"} to "my-index-*-*".
     * @param indexName the index name to replace
     * @return the replaced index name
     */
    public static String replaceIndexName(final String indexName) {
        logger.trace("replaceIndexName({})", indexName);
        String replaced =
                // We need to first URL decode the index name
                URLDecoder.decode(indexName, StandardCharsets.UTF_8)
                // We replace {WHATEVER} with *
                .replaceAll("\\{[^}]*\\}", "*")
                // We replace <WHATEVER> with WHATEVER
                .replaceAll("<([^>]*)>", "$1")
                // We replace the six ending digits like -123456 with -*
                .replaceAll("-\\d{6}", "-*");
        logger.trace("/replaceIndexName({}) = [{}]", indexName, replaced);
        return replaced;
    }
}
