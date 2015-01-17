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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * list resources available from the classpath @ *
 * From http://stackoverflow.com/questions/3923129/get-a-list-of-resources-from-classpath-directory
 */
public class ResourceList {
    private static final Logger logger = LogManager.getLogger(ResourceList.class);

    /**
     * @param root
     *            the pattern to match
     * @return the resources in the order they are found
     */
    public static Collection<String> getResources(
            final String root) {
        logger.trace("getResources({})", root);
        Pattern patternDir = Pattern.compile(".*");
        Pattern patternJar = Pattern.compile(".*");
        if (root != null) {
            patternDir = Pattern.compile(".*" + root + ".*");
            patternJar = Pattern.compile(root + ".*");
        }

        final ArrayList<String> retval = new ArrayList<>();
        final String classPath = System.getProperty("java.class.path", ".");
        final String[] classPathElements = classPath.split(File.pathSeparator);
        for (final String element : classPathElements) {
            retval.addAll(getResources(element, patternDir, patternJar, root));
        }
        return retval;
    }

    private static Collection<String> getResources(
            final String element,
            final Pattern patternDir,
            final Pattern patternJar,
            final String root) {
        final ArrayList<String> retval = new ArrayList<>();
        final File file = new File(element);
        if (file.isDirectory()) {
            retval.addAll(getResourcesFromDirectory(file, patternDir, element, root));
        } else {
            retval.addAll(getResourcesFromJarFile(file, patternJar));
        }
        return retval;
    }

    private static Collection<String> getResourcesFromJarFile(
            final File file,
            final Pattern pattern) {
        final ArrayList<String> retval = new ArrayList<>();
        ZipFile zf;
        try {
            zf = new ZipFile(file);
        } catch (final ZipException e) {
            throw new Error(e);
        } catch (final IOException e) {
            throw new Error(e);
        }
        final Enumeration<? extends ZipEntry> e = zf.entries();
        while(e.hasMoreElements()){
            final ZipEntry ze = e.nextElement();
            String fileName = ze.getName();
            final boolean accept = pattern.matcher(fileName).matches();
            if (accept) {
                // We should ignore dirs. They end with /
                if (!fileName.endsWith("/")) {
                    retval.add(fileName);
                }
            }
        }
        try {
            zf.close();
        } catch (final IOException e1) {
            throw new Error(e1);
        }
        return retval;
    }

    /**
     * TODO Use Path
     try (DirectoryStream<Path> stream = Files.newDirectoryStream(templatesDir)) {
        for (Path templatesFile : stream) {
        }
     }
     */
    private static Collection<String> getResourcesFromDirectory(
            final File directory,
            final Pattern pattern,
            final String element,
            final String root) {
        final ArrayList<String> retval = new ArrayList<>();
        final File[] fileList = directory.listFiles();
        for (final File file : fileList) {
            if (file.isDirectory()) {
                // We add the dirname itself
                try {
                    final String fileName = file.getCanonicalPath();
                    final boolean accept = pattern.matcher(fileName).matches();
                    if (accept) {
                        if (Pattern.compile(element + File.separator +  root + ".*").matcher(fileName).matches()) {
                            retval.add(fileName.substring((element + File.separator).length()));
                        }
                    }
                } catch (final IOException e) {
                    throw new Error(e);
                }
                retval.addAll(getResourcesFromDirectory(file, pattern, element, root));
            } else {
                try {
                    final String fileName = file.getCanonicalPath();
                    final boolean accept = pattern.matcher(fileName).matches();
                    if (accept) {
                        if (Pattern.compile(element + File.separator +  root + ".*").matcher(fileName).matches()) {
                            retval.add(fileName.substring((element + File.separator).length()));
                        }
                    }
                } catch (final IOException e) {
                    throw new Error(e);
                }
            }
        }
        return retval;
    }
}
