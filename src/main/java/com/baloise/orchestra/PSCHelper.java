package com.baloise.orchestra;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class PSCHelper {

	private static final String DEPLOYMENT_INFO = "__deploymentinfo__";
	public static final String DEFAULT_EXCLUDE = "DEFAULT";
	private Consumer<Object> log = System.out::println;

	private void zipDirectory(File dir, ZipOutputStream zOut, String exclude) throws IOException {
		zipDirectory("", dir, zOut, exclude);
	}

	private void zipDirectory(String basePath, File dir, ZipOutputStream zOut, String exclude) throws IOException {
		byte[] buffer = new byte[4096];
		File[] files = dir.listFiles();
		for (File file : files) {
			if (isExcluded(file, exclude)) {
				log.accept("excluded " + file.getPath());
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
		if(exclude.equals(DEFAULT_EXCLUDE)) {
			return file.isDirectory() || file.getName().startsWith(".") || file.getName().equalsIgnoreCase("pom.xml");
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

	public PSCHelper withLog(Consumer<Object> log) {
		this.log = log;
		return this;
	}

}
