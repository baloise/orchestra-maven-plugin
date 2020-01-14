package com.baloise.maven.orchestra;

import static com.baloise.maven.orchestra.MojoHelper.ajustOutputDir;
import static com.baloise.maven.orchestra.MojoHelper.getPscFileLocation;
import static com.baloise.maven.orchestra.MojoHelper.hasPom;
import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.baloise.orchestra.PSCHelper;

@Mojo(name = "scenario-package", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = false)
public class PackageMojo extends AbstractMojo {
	
	@Parameter(defaultValue = "${project.build.directory}", property = "outputDir", required = true)
	private File outputDirectory;
	
	@Parameter(property = "pscFile", required = false)
	private File pscFile;

	@Parameter(defaultValue = "${project.artifactId}", property = "artifactId", required = true)
	private String artifactId;

	@Parameter(defaultValue = "${project.version}", property = "version", required = true)
	private String version;

	public void execute() throws MojoExecutionException {
		outputDirectory  = ajustOutputDir(outputDirectory, getLog());
		try {
			File orchestraSrc = detectSourceFolder();
			if (!orchestraSrc.isDirectory())
				throw new IOException(orchestraSrc.getAbsolutePath() + " is not a directory");
			File pscFileLocation = getPscFileLocation(outputDirectory, artifactId, version, pscFile);
			getLog().info(format("packaging %s to %s", orchestraSrc, pscFileLocation));
			PSCHelper.createPscFile(orchestraSrc, pscFileLocation, artifactId);
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	public File detectSourceFolder() throws IOException {
		File defaultSource = new File("src/main/orchestra");
		if(hasPom()) {
			File ret = new File(defaultSource, artifactId);
			if(ret.isDirectory()) 
				return ret;
			else 
				getLog().warn(format("expected orchestra sources not found @ %s. running autodetection",ret));
		}
		Path path = defaultSource.isDirectory()? defaultSource.toPath()  : Paths.get(".");
		return Files
					.walk(path, 4)
					.filter(p -> p.getFileName().toString().equals("props"))
					.findAny()
					.map(Path::getParent)
					.orElseThrow(IOException::new)
					.toFile();
	}
}
