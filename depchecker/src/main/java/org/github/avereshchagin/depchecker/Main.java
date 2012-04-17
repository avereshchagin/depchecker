package org.github.avereshchagin.depchecker;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.graph.PreorderNodeListGenerator;

import java.util.ArrayList;
import java.util.List;

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
     * The entry point to Aether, i.e. the component doing all the work.
     *
     * @component
     */
    private RepositorySystem repoSystem;

    /**
     * The current repository/network configuration of Maven.
     *
     * @parameter default-value="${repositorySystemSession}"
     * @readonly
     */
    private RepositorySystemSession repoSession;

    /**
     * The project's remote repositories to use for the resolution of plugins and their dependencies.
     *
     * @parameter default-value="${project.remotePluginRepositories}"
     * @readonly
     */
    private List<RemoteRepository> remoteRepos;

    public void execute()
            throws MojoExecutionException {
        try {
            getLog().info("Depchecker: " + groupId + " : " + artifactId);
            DefaultArtifact artifact = new DefaultArtifact(groupId + ":" + artifactId + ":1.0-SNAPSHOT");

            recursiveList(artifact, 3);
        } catch (Exception e) {
            getLog().error("Depchecker: " + e.getMessage());
        }
    }

    private void recursiveList(Artifact artifact, int n) {
        if (n > 0) {
            List<Dependency> projectDependencies = getArtifactsDependencies(artifact);
            for (Dependency d : projectDependencies) {
                Artifact subArtifact = d.getArtifact();
                getLog().info("Depchecker: " + subArtifact.getGroupId());
                getLog().info("Depchecker: " + subArtifact.getArtifactId());
                recursiveList(subArtifact, n - 1);
            }
        }
    }

    private List<Dependency> getArtifactsDependencies(Artifact a) {
        List<Dependency> ret = new ArrayList<Dependency>();

        DefaultArtifact pomArtifact = new DefaultArtifact(a.getGroupId(), a.getArtifactId(), "pom", a.getVersion());
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(pomArtifact, "test"));
        collectRequest.setRepositories(remoteRepos);

        try {
            DependencyNode node = repoSystem.collectDependencies(repoSession, collectRequest).getRoot();
            DependencyRequest projectDependencyRequest = new DependencyRequest(node, null);

            repoSystem.resolveDependencies(repoSession, projectDependencyRequest);

            PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
            node.accept(nlg);

            ret.addAll(nlg.getDependencies(true));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ret;
    }
}
