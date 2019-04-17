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

package org.jaylen.maven.plugins.test;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.jaylen.maven.plugins.test.components.Artefactory;
import org.jaylen.maven.plugins.test.components.FileSys;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Mojo(name = "test", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class TestMojo extends AbstractMojo {

    /**
     * Base directory for all integration tests. Each test
     * should reside in a separate directory within this
     * folder.
     */
    @Parameter(required = true, property = "test.srcroot", defaultValue = "${basedir}/src/mock")
    private File root;

    /**
     * A directory where test maven projects will be copied
     * before execution.
     */
    @Parameter(required = true, property = "test.workspace", defaultValue = "${project.build.directory}/workspace")
    private File workspace;

    /**
     * A path to local maven repository used for tests.
     */
    @Parameter(required = true, property = "test.localrepo", defaultValue = "${project.build.directory}/repository")
    private File repository;

    /**
     * List of dependency scopes to install in the local repository.
     */
    @Parameter(required = true, property = "test.scopes", defaultValue = "compile,runtime")
    private List<String> scopes;

    /**
     * Additional parameters to be used during substitution
     * of properties in files of test projects.
     */
    @Parameter
    private Map<String, String> parameters;

    /**
     * Path to maven executable.
     */
    @Parameter(required = true, property = "test.mvn", defaultValue = "mvn")
    private String mvn;

    /**
     * Maven command line options.
     */
    @Parameter(required = true, property = "test.options", defaultValue = "--batch-mode,--errors")
    private List<String> options;

    /**
     * Additional maven defines to be used in command line
     * with {@code --define} or {@code -D} flags.
     */
    @Parameter
    private Map<String, String> defines;

    /**
     * Name of log file to save build output to.
     * This file will be located in the root of test project
     * copied into workspace directory.
     */
    @Parameter(required = true, property = "test.logfile", defaultValue = "build.log")
    private String logfile;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        install();
        copy(params());
    }

    private void install() throws MojoFailureException, MojoExecutionException {
        artefactory.installPom(project, repository, session.getProjectBuildingRequest());
        artefactory.install(project.getArtifact(), repository, session.getProjectBuildingRequest());
        final Collection<Artifact> dependencies = project.getArtifacts().stream().filter(x -> scopes.contains(x.getScope())).collect(Collectors.toList());
        for (final Artifact x : dependencies) {
            artefactory.install(x, repository, session.getProjectBuildingRequest());
        }
    }

    private void copy(final Map<String, String> params) throws MojoExecutionException {
        fs.mkdir(workspace);
        final Collection<Path> projects = fs.ls(root.toPath()).map(p -> fs.copy(p, workspace.toPath(), params)).collect(Collectors.toList());
        for (final Path p : projects) {
            run(p.toFile());
        }
    }

    private Map<String, String> params() {
        final Map<String, String> params = new HashMap<>();
        params.put("current-group-id", project.getGroupId());
        params.put("current-artifact-id", project.getArtifactId());
        params.put("current-version", project.getVersion());
        if (parameters != null) {
            params.putAll(parameters);
        }
        return params;
    }

    private void run(final File project) throws MojoExecutionException {
        fs.mkdir(root);
        getLog().info("building " + project.getName());
        final List<String> cmd = command(project);
        final ProcessBuilder maven = new ProcessBuilder(cmd)
                .directory(project)
                .redirectErrorStream(true)
                .redirectOutput(project.toPath().resolve(logfile).toFile());
        try {
            getLog().info(String.join(" ", cmd));
            final Process process = maven.start();
            if (process.waitFor() == 0) {} else {
                throw new MojoExecutionException("external command return non-zero code");
            }
        } catch (final IOException fault) {
            throw new MojoExecutionException("unexpected fault while running external command", fault);
        } catch (final InterruptedException fault) {
            throw new MojoExecutionException("process has been interrupted", fault);
        }
    }

    private List<String> command(final File cwd) {
        final Map<String, String> params = new HashMap<>();
        if (defines != null) {
            params.putAll(defines);
        }
        params.put("maven.repo.local", cwd.toPath().relativize(repository.toPath()).toString());
        final List<String> cmd = new ArrayList<>();
        cmd.add(mvn);
        cmd.addAll(options);
        cmd.addAll(params.entrySet().stream().map(x -> "-D" + x.getKey() + "=" + x.getValue()).collect(Collectors.toList()));
        cmd.addAll(Arrays.asList("clean", "package"));
        return cmd;
    }

    @Component
    private Artefactory artefactory;

    @Component
    private FileSys fs;

    @Parameter(readonly = true, required = true, defaultValue = "${basedir}")
    private File basedir;

    @Parameter(readonly = true, required = true, defaultValue = "${project.build.directory}")
    private File builddir;

    @Parameter(readonly = true, required = true, defaultValue = "${project}")
    private MavenProject project;

    @Parameter(readonly = true, required = true, defaultValue = "${session}")
    private MavenSession session;

}
