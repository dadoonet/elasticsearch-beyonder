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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashSet;
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
    private static final Logger logger = LogManager.getLogger(ResourceList.class);
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
            return new File(dirURL.toURI()).list();
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
            JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
            Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
            Set<String> result = new HashSet<>(); //avoid duplicates in case it is a subdirectory
            while(entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith(root)) { //filter according to the path
                    String entry = name.substring(root.length());
                    int checkSubdir = entry.indexOf("/");
                    if (checkSubdir >= 0) {
                        // if it is a subdirectory, we just return the directory name
                        entry = entry.substring(0, checkSubdir);
                    }
                    result.add(entry);
                }
            }
            return result.toArray(new String[result.size()]);
        }

        // Resource does not exist. We can return an empty list
        logger.trace("did not find any resource. returning empty array");
        return NO_RESOURCE;
    }
}
