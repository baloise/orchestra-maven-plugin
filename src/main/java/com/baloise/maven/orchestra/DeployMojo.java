package com.baloise.maven.orchestra;

import static com.baloise.maven.orchestra.MojoHelper.getPscFileLocation;
import static java.lang.String.format;

import java.io.File;
import java.net.URI;

import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.baloise.orchestra.DeployHelper;
import com.baloise.orchestra.LandscapeAdminHelper;

@Mojo(name = "scenario-deploy", defaultPhase = LifecyclePhase.NONE, requiresProject = false)
public class DeployMojo extends AbstractMojo {
	
	@Parameter(defaultValue = "${project.build.directory}", property = "outputDir", required = true)
	private File outputDirectory;
	
	@Parameter(property = "pscFile", required = false)
	private File pscFile;
	
	@Parameter(property = "landscapeFile", required = false)
	private File landscapeFile;

	@Parameter(defaultValue = "${project.artifactId}", property = "artifactId", required = true)
	private String artifactId;

	@Parameter(defaultValue = "${project.version}", property = "version", required = true)
	private String version;

	@Parameter(property = "user", required = true)
	private String user;
	
	@Parameter(property = "comment", required = false)
	private String comment;

	@Parameter(property = "password", required = true)
	private String password;

	@Parameter(property = "server", required = true)
	private String server;
	
	public void execute() throws MojoExecutionException {
		try {
			DeployHelper deployHelper = new DeployHelper(user, password, new URI(server))
					.withComment(comment)
					.withLog(o -> getLog().info(String.valueOf(o)));
			File pscFileLocation = getPscFileLocation(outputDirectory, artifactId, version, pscFile);
			getLog().info(format("deploying %s to %s", pscFileLocation, server));
			String scenarioId = deployHelper.deploy(pscFileLocation);
			if(landscapeFile!=null) {
				LandscapeAdminHelper landscapeHelper = new LandscapeAdminHelper(user, password, new URI(server)).withLog(deployHelper.getLog());
				INIConfiguration ini = new Configurations().ini(landscapeFile);
				landscapeHelper.deploy(scenarioId, ini);
			}
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

}
