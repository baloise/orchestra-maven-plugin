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

import com.baloise.orchestra.DeployHelper;
import com.baloise.orchestra.LandscapeAdminHelper;


/**
 * Deploys ( or redeploys ) a scenario ( PSC file ).<br/><br/>
 * Example usage:
 * <pre>mvn com.baloise.maven:orchestra-maven-plugin scenario-deploy -Duser=admin -Dpassword=*** -Dserver=https://orchestra.example.com:8443 -DpscFile=/tmp/my.psc -landscapeFile=/tmp/landscape.json</pre>
 * If you use a pom.xml feel free to omit <i>pscFile</i><br/>
 */
@Mojo(name = "scenario-deploy", defaultPhase = LifecyclePhase.NONE, requiresProject = false)
public class DeployMojo extends AbstractMojo {
	
    /**
     * the directory from where the PSC file will be read.<br/>
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
	 * Full path to the landscape file to deploy.<br/>
	 * Each existing landscape entry will be overwritten.<br/>
	 * If you don't give a property name ( see <i>ee_mailto</i> ) the default name <code>VALUE</code> will be used.<br/>
	 * Property names are not case sensitive.<br/>
	 * Arrays are joined with ", " (comma, blank) as separator.<br/>
	 * <pre>{
    "ee_openhr": {
        "host": "myserver.example.com",
        "port": "443"
    },
    "ee_mailto": [
        "receiver1@example.com",
        "second_receiver@example.com",
    ]
}</pre>
	 * You can also put several landscapes into the same JSON and select them with<br/>
	 * (on Microsoft Windows systems use ';' instead of ':')
	 * <pre>-DlandscapeFile=path/to/landscape.json:env/test</pre>
	 *<pre>{"env": { "test" : { ... test landscape here ... }}}</pre>
	 * @since 0.4.0 
	 */
	@Parameter(property = "landscapeFile", required = false)
	private String landscapeFile;

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

	/**
	 * @since 0.4.0 
	 */
	@Parameter(property = "user", required = true)
	private String user;
	
	/**
	 * @since 0.4.0 
	 */
	@Parameter(property = "comment", required = false)
	private String comment;

	/**
	 * @since 0.4.0 
	 */
	@Parameter(property = "password", required = true)
	private String password;
	
	/**
	 * @since 0.5.2 
	 */
	@Parameter(defaultValue = "1000", property = "retryDelayMillies", required = false)
	private long retryDelayMillies;
	
	/**
	 * @since 0.5.2 
	 */
	@Parameter(defaultValue = "30", property = "retryCount", required = false)
	private int retryCount;

	/**
	 * Full server URL with protocol and web service port.<br/>
	 * Please note the web UI and web service do not run on the same port.<br/>
	 * Example: <i>https://orchestra.example.com:8443</i>
	 * Example: <i>https://orchestra.example.com:8443</i>
	 * @since 0.4.0 
	 */
	@Parameter(property = "server", required = true)
	private String servers;
	
	public void execute() throws MojoExecutionException {
		try {
			DeployHelper deployHelper = new DeployHelper(user, password, servers)
					.withComment(comment)
					.withLog(new ApacheLogWrapper(getLog()))
					.withRetryDelayMillies(retryDelayMillies)
					.withRetryCount(retryCount);
			File pscFileLocation = getPscFileLocation(outputDirectory, artifactId, version, pscFile);
			getLog().info(format("deploying %s to %s", pscFileLocation, servers));
			String scenarioId = deployHelper.deploy(pscFileLocation);
			if(landscapeFile!=null) {
				LandscapeAdminHelper landscapeHelper = new LandscapeAdminHelper(user, password, servers).withLog(deployHelper.getLog());
				landscapeHelper.deploy(scenarioId, landscapeFile);
			}
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

}
