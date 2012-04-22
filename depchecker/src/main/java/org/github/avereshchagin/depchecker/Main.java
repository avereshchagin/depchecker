package org.github.avereshchagin.depchecker;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.graph.PreorderNodeListGenerator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
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
            getLog().info("Collection dependencies by parsing POM");
            printDependenciesFromPom();
            getLog().info("Collection dependencies using API");
            printDependenciesFromTree();
        } catch (Exception e) {
            getLog().error("Depchecker: " + e.getMessage());
        }
    }

    private void printDependenciesFromPom()
            throws ArtifactResolutionException, ParserConfigurationException, IOException,
            SAXException, XPathExpressionException {
        Artifact artifact = new DefaultArtifact(groupId, artifactId, "pom", "1.0-SNAPSHOT");

        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(artifact);
        request.setRepositories(remoteRepos);
        getLog().info("Resolving artifact " + artifact + " from " + remoteRepos);

        ArtifactResult result = repoSystem.resolveArtifact(repoSession, request);
        getLog().info("Resolved artifact " + artifact + " to " +
                result.getArtifact().getFile() + " from "
                + result.getRepository());

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(result.getArtifact().getFile());

        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPathExpression expression = xPathFactory.newXPath().compile("//project/dependencies/dependency");
        XPathExpression artifactIdPath = xPathFactory.newXPath().compile("artifactId/text()");
        XPathExpression groupIdPath = xPathFactory.newXPath().compile("groupId/text()");
        XPathExpression versionPath = xPathFactory.newXPath().compile("version/text()");

        NodeList nodeList = (NodeList) expression.evaluate(document, XPathConstants.NODESET);
        getLog().info("Number of dependencies: " + nodeList.getLength());
        for (int i = 0; i < nodeList.getLength(); i++) {
            String groupIdString = (String) groupIdPath.evaluate(nodeList.item(i), XPathConstants.STRING);
            String artifactIdString = (String) artifactIdPath.evaluate(nodeList.item(i), XPathConstants.STRING);
            String versionString = (String) versionPath.evaluate(nodeList.item(i), XPathConstants.STRING);
            getLog().info("Dependency: " + groupIdString + ":" + artifactIdString + ":" + versionString);
        }
    }

    private void printDependenciesFromTree()
            throws DependencyCollectionException {
        DefaultArtifact pomArtifact = new DefaultArtifact(groupId, artifactId, "pom", "1.0-SNAPSHOT");

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(pomArtifact, null));
        collectRequest.setRepositories(remoteRepos);

        DependencyNode node = repoSystem.collectDependencies(repoSession, collectRequest).getRoot();
        List<DependencyNode> nodeList = node.getChildren();
        getLog().info("Number of dependencies: : " + nodeList.size());
        for (DependencyNode n : nodeList) {
            getLog().info("Dependency: " + n.getDependency().getArtifact());
        }
    }
}
