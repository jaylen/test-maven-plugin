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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.transfer.artifact.install.ArtifactInstaller;
import org.apache.maven.shared.transfer.artifact.install.ArtifactInstallerException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import java.io.File;
import java.util.Collections;

@Component(role = Artefactory.class, instantiationStrategy = "singleton")
public class Artefactory {

    /**
     * Installs artefact for pom file of the given maven project.
     */
    public void installPom(final MavenProject project, final File repo, final ProjectBuildingRequest request) throws MojoFailureException, MojoExecutionException {
        final Artifact pom = factory.createArtifact(project.getGroupId(), project.getArtifactId(), project.getVersion(), "pom");
        pom.setFile(project.getFile());
        install(pom, repo, request);
    }

    /**
     * Installs artefact into specific local repository.
     */
    public void install(final Artifact artifact, final File repo, final ProjectBuildingRequest request) throws MojoExecutionException, MojoFailureException {
        if (artifact == null) {
            throw new MojoFailureException("artefact must not be null");
        }
        if (artifact.getFile() == null || !artifact.getFile().exists()) {
            throw new MojoExecutionException("artefact " + artifact.getId() + " has not been packaged yet");
        }
        fs.mkdir(repo);
        try {
            installer.install(request, repo, Collections.singletonList(artifact));
        } catch (final ArtifactInstallerException fault) {
            throw new MojoExecutionException("unable to install artefact " + artifact.getId(), fault);
        }
    }
    @Requirement
    private ArtifactInstaller installer;

    @Requirement
    private RepositorySystem factory;

    @Requirement
    private FileSys fs;

}
