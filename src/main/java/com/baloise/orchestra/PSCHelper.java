package com.baloise.orchestra;

import static java.lang.String.format;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class PSCHelper {

	private static final String DEPLOYMENT_INFO = "__deploymentinfo__";
	public static final String DEFAULT_EXCLUDE = "DEFAULT";
	private Log log = Log.DEFAULT;

	private void zipDirectory(File dir, ZipOutputStream zOut, String exclude) throws IOException {
		zipDirectory("", dir, zOut, exclude);
	}

	private void zipDirectory(String basePath, File dir, ZipOutputStream zOut, String exclude) throws IOException {
		byte[] buffer = new byte[4096];
		File[] files = dir.listFiles();
		for (File file : files) {
			if (isExcluded(file, exclude)) {
				log.info("excluded " + file.getPath());
			}else if (file.isDirectory()) {
				String path = basePath + file.getName() + "/";
				zOut.putNextEntry(new ZipEntry(path));
				zipDirectory(path, file, zOut, exclude);
				zOut.closeEntry();
			} else {
				FileInputStream fin = new FileInputStream(file);
				zOut.putNextEntry(new ZipEntry(basePath + file.getName()));
				int length;
				while ((length = fin.read(buffer)) > 0) {
					zOut.write(buffer, 0, length);
				}
				zOut.closeEntry();
				fin.close();
			}
		}
	}

	private boolean isExcluded(File file, String exclude) {
		if(exclude == null) return false;
		if(exclude.equals(DEFAULT_EXCLUDE)) {
			if(file.isDirectory()) return true;
			String lname = file.getName().toLowerCase();
			if(lname.startsWith(".")) return true;
			if(lname.startsWith("readme")) return true;
			if(lname.equals("pom.xml")) return true;
			if(lname.endsWith(".psc")) return true;
		}
		return false;
	}

	public String getScenarioId(File psc) throws IOException {
		return getScenarioProperties(psc).getProperty("UUID");
	}
	
	public Properties getScenarioProperties(File psc) throws IOException {
		try (ZipFile zipFile = new ZipFile(psc)) {
			ZipEntry entry = zipFile.getEntry("props");
			try (InputStream in = zipFile.getInputStream(entry)) {
				Properties ret = new Properties();
				ret.load(in);
				return ret;
			}
		}
	}
	
	
	
	public void createPscFile(File sourceFolder, File targetFile, String scenarioName, String exclude) throws IOException {
		if(toAbsoluteNormalPath(targetFile).startsWith(toAbsoluteNormalPath(sourceFolder))) {
			throw new IllegalArgumentException(format("Endless loop detected : target file %s must not be under source folder %s", targetFile, sourceFolder));
		}
		FileOutputStream fOut = new FileOutputStream(targetFile);
		CheckedOutputStream checksum = new CheckedOutputStream(fOut, new Adler32());
		ZipOutputStream zOut = new ZipOutputStream(checksum);
		ZipEntry zipEntry = new ZipEntry(DEPLOYMENT_INFO);
		zipEntry.setTime(System.currentTimeMillis());
		zOut.putNextEntry(zipEntry);

		DataOutputStream dOut = new DataOutputStream(zOut);
		dOut.writeUTF(scenarioName);

		zipDirectory(sourceFolder, zOut, exclude);

		dOut.flush();
		zOut.closeEntry();
		zOut.finish();
		zOut.close();
	}

	private Path toAbsoluteNormalPath(File targetFile) {
		return targetFile.getAbsoluteFile().toPath().normalize();
	}

	public PSCHelper withLog(Log log) {
		this.log = log;
		return this;
	}

}
