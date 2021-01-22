package com.baloise.maven.orchestra;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.baloise.orchestra.LandscapeAdminHelper;

/**
 * @since 0.6.0
 */
@Mojo(name = "landscape-get", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, requiresProject = false)
public class LandscapeMojo extends AbstractMojo {
	
	
	/**
	 * the directory where the PSC file will be written.<br/>
	 * If you use the {@link #pscFile} property, this will be ignored.
	 * @since 0.6.0 
	 */
	@Parameter(defaultValue = "${project.build.directory}", property = "outputDir", required = true)
	private File outputDirectory;
	

	/**
	 * @since 0.6.0 
	 */
	@Parameter(property = "uris", required = true)
	private String uris;
	
	/**
	 * @since 0.6.0 
	 */
	@Parameter(property = "scenarioId", required = true)
	private String scenarioId;
	
	/**
	 * @since 0.6.0 
	 */
	@Parameter(defaultValue = "****", property = "mask", required = false)
	private String mask;

	public void execute() throws MojoExecutionException {
		outputDirectory  = MojoHelper.ajustOutputDir(outputDirectory, getLog());
		try {
			File landscapeFile = new File(outputDirectory, scenarioId+".json");
			getLog().info(format("saving landscapes for scenario %s to %s", scenarioId, landscapeFile));
			LandscapeAdminHelper.getLandscapesAsJson(uris,scenarioId,mask, landscapeFile);
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}
	
	File ajustOutputDir(File outputDirectory) {
		if(!outputDirectory.isDirectory()) {
			getLog().info("invalid outputDir "+outputDirectory+". using cwd.");	
			return new File(".");
		}
		return outputDirectory;
	}
	
	
}
