package com.baloise.maven.orchestra;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Base64;

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
	public interface Decoder {
		String decode(String input);
	}

	public enum Encoding implements Decoder {
		URL {
			public String decode(String input) {
				try {
					return URLDecoder.decode(input, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					throw new IllegalStateException(e);
				}
			}
		},
		BASE64 {
			public String decode(String input) {
				return new String(Base64.getDecoder().decode(input));
			}
		},
		NONE {
			public String decode(String input) {
				return input;
			}
		}
	}

	/**
	 * the directory where the PSC file will be written.<br/>
	 * If you use the {@link #pscFile} property, this will be ignored.
	 * 
	 * @since 0.6.0
	 */
	@Parameter(defaultValue = "${project.build.directory}", property = "outputDir", required = true)
	private File outputDirectory;

	/**
	 * separated by semicolon
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

	/**
	 * @since 0.7.2
	 */
	@Parameter(defaultValue = "NONE", property = "maskEncoding", required = false)
	private Encoding maskEncoding;

	public void execute() throws MojoExecutionException {
		if(getLog().isDebugEnabled()) {
			getLog().debug("Mask is " + mask);
			getLog().debug("MaskEncoding is " + maskEncoding);
			getLog().debug("decoded Mask is " + maskEncoding.decode(mask));
		}
		outputDirectory = MojoHelper.ajustOutputDir(outputDirectory, getLog());
		try {
			File landscapeFile = new File(outputDirectory, scenarioId + ".json");
			getLog().info(format("saving landscapes for scenario %s to %s", scenarioId, landscapeFile));
			LandscapeAdminHelper.getLandscapesAsJson(uris, scenarioId, maskEncoding.decode(mask), landscapeFile);
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	File ajustOutputDir(File outputDirectory) {
		if (!outputDirectory.isDirectory()) {
			getLog().info("invalid outputDir " + outputDirectory + ". using cwd.");
			return new File(".");
		}
		return outputDirectory;
	}

}
