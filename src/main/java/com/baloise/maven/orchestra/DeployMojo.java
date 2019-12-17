package com.baloise.maven.orchestra;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Goal which touches a timestamp file.
 */
@Mojo(name = "scenario-deploy", defaultPhase = LifecyclePhase.NONE, requiresProject = false)
public class DeployMojo extends AbstractMojo {
	/**
	 * Location of the file.
	 */
	@Parameter(defaultValue = "${project.build.directory}", property = "outputDir", required = true)
	private File outputDirectory;

	public void execute() throws MojoExecutionException {

	}
}
