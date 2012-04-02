package org.github.avereshchagin.depchecker;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @goal depchecker
 * @phase process-sources
 */
public class Main extends AbstractMojo {
    /**
     * @parameter expression="${depchecker.artifactId}"
     * @required
     */
    private String artifactId;

    /**
     * @parameter expression="${depchecker.groupId}"
     * @required
     */
    private String groupId;

    /**
     * @parameter expression="${project}"
     * @readonly
     */
    protected MavenProject project;

    /**
     * @component role="org.apache.maven.project.MavenProjectBuilder"
     * @required
     * @readonly
     */
    protected MavenProjectBuilder mavenProjectBuilder;

    /**
     * @parameter expression="${localRepository}"
     * @readonly
     * @required
     */
    protected ArtifactRepository local;

    /**
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @readonly
     * @required
     */
    protected List<ArtifactRepository> remoteRepos;

    /**
     * @component
     */
    protected ArtifactFactory factory;

    public void execute()
            throws MojoExecutionException {
        try {
            getLog().info("Depchecker: " + groupId + " : " + artifactId);
            Artifact pomArtifact = this.factory.createArtifact(groupId, artifactId,
                    Artifact.SNAPSHOT_VERSION, "", "pom");
            MavenProject pomProject = mavenProjectBuilder.buildFromRepository(pomArtifact, remoteRepos, local);
            resolveDependencies(pomProject);
        } catch (Exception e) {
            getLog().error("Depchecker: " + e.getMessage());
        }
    }

    private void resolveDependencies(MavenProject project)
            throws InvalidDependencyVersionException, ProjectBuildingException {
        Set<Artifact> artifacts = project.createArtifacts(this.factory, Artifact.SCOPE_TEST,
                new ScopeArtifactFilter(Artifact.SCOPE_TEST));
        for (Artifact artifact : artifacts) {
            getLog().info("Depchecker: " + artifact.getGroupId() + " : " + artifact.getArtifactId());
            Artifact pomArtifact = this.factory.createArtifact(artifact.getGroupId(), artifact.getArtifactId(),
                    artifact.getVersion(), "", "pom");
            MavenProject pomProject = mavenProjectBuilder.buildFromRepository(pomArtifact, remoteRepos, local);
            resolveDependencies(pomProject);
        }
    }
}
