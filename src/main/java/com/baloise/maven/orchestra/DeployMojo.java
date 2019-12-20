package com.baloise.maven.orchestra;

import static com.baloise.maven.orchestra.MojoHelper.getPscFileLocation;
import static java.lang.String.format;

import java.io.File;
import java.net.URI;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "scenario-deploy", defaultPhase = LifecyclePhase.NONE, requiresProject = false)
public class DeployMojo extends AbstractMojo {
	
	@Parameter(defaultValue = "${project.build.directory}", property = "outputDir", required = true)
	private File outputDirectory;
	
	@Parameter(property = "pscFile", required = false)
	private File pscFile;

	@Parameter(defaultValue = "${project.artifactId}", property = "artifactId", required = true)
	private String artifactId;

	@Parameter(defaultValue = "${project.version}", property = "version", required = true)
	private String version;

	@Parameter(property = "user", required = true)
	private String user;

	@Parameter(property = "password", required = true)
	private String password;

	@Parameter(property = "server", required = true)
	private String server;
	
	public void execute() throws MojoExecutionException {
		try {
			DeployHelper deployHelper = new DeployHelper(user, password, new URI(server));
			File pscFileLocation = getPscFileLocation(outputDirectory, artifactId, version, pscFile);
			getLog().info(format("deploying %s to %s", pscFileLocation, server));
			deployHelper.deploy(pscFileLocation);
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

}
