package com.baloise.maven.orchestra;

import static com.baloise.maven.orchestra.MojoHelper.getPscFileLocation;
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

/**
 * <h5>Maven layout</h5>
 * Put your PSC source files under /src/main/orchestra and run
 * <pre>mvn com.baloise.maven:orchestra-maven-plugin scenario-package</pre>
 * to package PSC file into the target folder.<br/>
 * <br/>
 * or <a href="plugin-info.html#Usage">include the plugin in your pom.xml</a> and run
 * <pre>mvn package</pre>
 * <br/>
 * <h5>Package PSC source folder without Maven project layout / POM</h5>
 * <pre>mvn -DpscFile=/path/to/my.psc -DsourceFolder=/path/to/myPSCsource -Dexclude=DEFAULT com.baloise.maven:orchestra-maven-plugin scenario-package</pre>
 * Exclude parameter is recommended so you don't accidentally package .git folder or similar.<br/>
 */
@Mojo(name = "scenario-package", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = false)
public class PackageMojo extends AbstractMojo {
	
	
	/**
	 * the directory where the PSC file will be written.<br/>
	 * If you use the {@link #pscFile} property, this will be ignored.
	 * @since 0.4.0 
	 */
	@Parameter(defaultValue = "${project.build.directory}", property = "outputDir", required = true)
	private File outputDirectory;
	
	/**
	 * full path to where is PSC file will be written.<br/> 
	 * Overwrites {@link #outputDir}
	 * @since 0.4.0 
	 */
	@Parameter(property = "pscFile", required = false)
	private File pscFile;
	
	/**
	 * The folder to be packaged to PSC.<br/>
	 * If omitted, the following logic is applied<br/>
	 * <ol>
	 * <li>if running <b>with</b> a POM -> src/main/orchestra/<b>${artifactId}</b></li>
	 * <li>if running without a POM -> search in src/main/orchestra if exists or the current path otherwise. Stop at the first occurrence of <i>props</i>-file, take the parent folder. Max directory depth is 4.</li>
	 * </ol>
	 * @since 0.4.0 
	 */
	@Parameter(property = "sourceFolder", required = false)
	private File sourceFolder;
		
	/**
	 * If set to <code>"DEFAULT"</code> the following will be excluded from the PSC file
	 * <ul>
	 * <li>all directories</li>
	 * <li>all files starting with dot (".")</li>
	 * <li>all files named <i>pom.xml</i></li>
	 * </ul>
	 * @since 0.4.0 
	 */
	@Parameter(property = "exclude", required = false)
	private String exclude;

	/**
	 * @since 0.4.0 
	 */
	@Parameter(defaultValue = "${project.artifactId}", property = "artifactId", required = true)
	private String artifactId;

	/**
	 * @since 0.4.0 
	 */
	@Parameter(defaultValue = "${project.version}", property = "version", required = true)
	private String version;

	public void execute() throws MojoExecutionException {
		if(pscFile == null) outputDirectory  = ajustOutputDir(outputDirectory);
		try {
			File orchestraSrc = detectSourceFolder();
			if (!orchestraSrc.isDirectory())
				throw new IOException(orchestraSrc.getAbsolutePath() + " is not a directory");
			File pscFileLocation = getPscFileLocation(outputDirectory, artifactId, version, pscFile);
			getLog().info(format("packaging %s to %s", orchestraSrc, pscFileLocation));
			new PSCHelper().withLog(new ApacheLogWrapper(getLog())).createPscFile(orchestraSrc, pscFileLocation, artifactId, exclude);
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	/**
	 * @return true if the execution runs within a project
	 */
	boolean hasProject() {
		// see https://github.com/apache/maven/blob/master/maven-core/src/main/resources/org/apache/maven/project/standalone.xml
		return "standalone-pom".equals(artifactId);
	}
	
	File ajustOutputDir(File outputDirectory) {
		if(!outputDirectory.isDirectory()) {
			getLog().info("invalid outputDir "+outputDirectory+". using cwd.");	
			return new File(".");
		}
		return outputDirectory;
	}
	
	public File detectSourceFolder() throws IOException {
		if(sourceFolder != null) return sourceFolder;
		File defaultSource = new File("src/main/orchestra");
		if(hasProject()) {
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
