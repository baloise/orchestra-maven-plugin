package com.baloise.maven.orchestra;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Goal which touches a timestamp file.
 */
@Mojo(name = "scenario-package", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = false)
public class PackageMojo extends AbstractMojo {
	/**
	 * Location of the file.
	 */
	@Parameter(defaultValue = "${project.build.directory}", property = "outputDir", required = true)
	private File outputDirectory;

	@Parameter(defaultValue = "${project.groupId}", property = "groupId", required = true)
	private String groupId;

	@Parameter(defaultValue = "${project.artifactId}", property = "artifactId", required = true)
	private String artifactId;

	@Parameter(defaultValue = "${project.version}", property = "version", required = true)
	private String version;

	public void execute() throws MojoExecutionException {
		try {
			System.out.println(groupId);
			System.out.println(artifactId);
			System.out.println(version);
			File orchestraSrc = new File("src/main/orchestra", artifactId);
			if (!orchestraSrc.isDirectory())
				throw new IOException(orchestraSrc.getAbsolutePath() + " is not a directory");
			File targetFile = new File(outputDirectory, artifactId + "-" + version + ".zip");
			PSCHelper.createPscFile(orchestraSrc, targetFile, artifactId);
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}
}
