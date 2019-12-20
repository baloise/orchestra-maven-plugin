package com.baloise.maven.orchestra;

import java.io.File;

import org.apache.maven.plugin.logging.Log;

public class MojoHelper {

	static File ajustOutputDir(File outputDirectory, Log log) {
		if(!outputDirectory.isDirectory()) {
			log.info("invalid outputDir parameter. using cwd.");	
			return new File(".");
		}
		return outputDirectory;
	}

	static boolean hasPom() {
		return new File("pom.xml").exists();
	}

	static File getPscFileLocation(File outputDirectory, String artifactId, String version, File pscFile ) {
		return pscFile == null ? new File(outputDirectory, artifactId + "-" + version + ".psc") : pscFile;
	}

}
