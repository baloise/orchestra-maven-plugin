package com.baloise.maven.orchestra;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

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

	@Parameter(defaultValue = "${project.artifactId}", property = "artifactId", required = true)
	private String artifactId;

	@Parameter(defaultValue = "${project.version}", property = "version", required = true)
	private String version;

	public void execute() throws MojoExecutionException {
		try {
			System.out.println(outputDirectory);
			System.out.println(artifactId);
			System.out.println(version);
			File orchestraSrc = detectSourceFolder();
			if (!orchestraSrc.isDirectory())
				throw new IOException(orchestraSrc.getAbsolutePath() + " is not a directory");
			File targetFile = new File(outputDirectory, artifactId + "-" + version + ".psc");
			PSCHelper.createPscFile(orchestraSrc, targetFile, artifactId);
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	public File detectSourceFolder() throws IOException {
		if(hasPom()) {
			return new File("src/main/orchestra", artifactId);
		}
		return Files
					.walk(Paths.get("."), 4)
					.filter(p -> p.getFileName().toString().equals("props"))
					.findAny()
					.map(Path::getParent)
					.orElseThrow(IOException::new)
					.toFile();
	}
	
	private boolean hasPom() {
		return artifactId != null && !"standalone-pom".equalsIgnoreCase(artifactId);
	}
}
