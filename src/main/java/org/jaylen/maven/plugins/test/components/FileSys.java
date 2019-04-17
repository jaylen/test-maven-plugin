/*
 * Copyright 2019 Yury Khrustalev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jaylen.maven.plugins.test.components;

import org.apache.commons.text.StringSubstitutor;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.component.annotations.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component(role = FileSys.class, instantiationStrategy = "singleton")
public class FileSys {

    public void mkdir(final File directory) throws MojoExecutionException {
        mkdir(directory.toPath());
    }

    public void mkdir(final Path directory) throws MojoExecutionException {
        if (Files.notExists(directory)) {
            try {
                Files.createDirectories(directory);
            } catch (final IOException fault) {
                throw new MojoExecutionException("unable to create directory " + directory, fault);
            }
        }
    }

    public Stream<Path> ls(final Path directory) throws MojoExecutionException {
        try {
            return Files.list(directory).filter(x -> Files.isDirectory(x));
        } catch (final IOException fault) {
            throw new MojoExecutionException("unexpected fault while listing directory", fault);
        }
    }

    public Path copy(final Path directory, final Path destination, final Map<String, String> params) {
        try {
            Files.walk(directory).forEach(path -> {
                final Path dest = destination.resolve(directory.getFileName().resolve(directory.relativize(path)));
                if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                    try {
                        Files.createDirectories(dest);
                    } catch (final IOException fault) {
                        throw new RuntimeException("unable to create a directory", fault);
                    }
                } else if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                    try {
                        filter(path, dest, params);
                    } catch (final IOException fault) {
                        throw new RuntimeException("unable to copy a file", fault);
                    }
                }
            });
            return destination.resolve(directory.getFileName());
        } catch (final IOException fault) {
            throw new RuntimeException("unable to travers directory recursively", fault);
        }
    }

    private void filter(final Path source, final Path dest, final Map<String, String> params) throws IOException {
        final String template = Files.lines(source).collect(Collectors.joining("\n"));
        final String text = StringSubstitutor.replace(template, params, "${{", "}}");
        Files.write(dest, text.getBytes());
    }

}
