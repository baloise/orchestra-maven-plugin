package com.baloise.maven.orchestra;

import java.io.File;

public class MojoHelper {

	static File getPscFileLocation(File outputDirectory, String artifactId, String version, File pscFile ) {
		return pscFile == null ? new File(outputDirectory, artifactId + "-" + version + ".psc") : pscFile;
	}

}
