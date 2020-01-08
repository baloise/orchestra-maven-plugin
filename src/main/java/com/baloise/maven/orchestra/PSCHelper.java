package com.baloise.maven.orchestra;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class PSCHelper {

	private static final String DEPLOYMENT_INFO = "__deploymentinfo__";

	private static void zipDirectory(File dir, ZipOutputStream zOut) throws IOException {
		zipDirectory("", dir, zOut);
	}

	private static void zipDirectory(String basePath, File dir, ZipOutputStream zOut) throws IOException {
		byte[] buffer = new byte[4096];
		File[] files = dir.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				String path = basePath + file.getName() + "/";
				zOut.putNextEntry(new ZipEntry(path));
				zipDirectory(path, file, zOut);
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

	public static String getScenarioId(File psc) throws IOException {
		return getScenarioProperties(psc).getProperty("UUID");
	}
	public static Properties getScenarioProperties(File psc) throws IOException {
		try (ZipFile zipFile = new ZipFile(psc)) {
			ZipEntry entry = zipFile.getEntry("props");
			try (InputStream in = zipFile.getInputStream(entry)) {
				Properties ret = new Properties();
				ret.load(in);
				return ret;
			}
		}
	}
	
	public static void createPscFile(File sourceFolder, File targetFile, String scenarioName) throws IOException {
		FileOutputStream fOut = new FileOutputStream(targetFile);
		CheckedOutputStream checksum = new CheckedOutputStream(fOut, new Adler32());
		ZipOutputStream zOut = new ZipOutputStream(checksum);
		ZipEntry zipEntry = new ZipEntry(DEPLOYMENT_INFO);
		zipEntry.setTime(System.currentTimeMillis());
		zOut.putNextEntry(zipEntry);

		DataOutputStream dOut = new DataOutputStream(zOut);
		dOut.writeUTF(scenarioName);

		zipDirectory(sourceFolder, zOut);

		dOut.flush();
		zOut.closeEntry();
		zOut.finish();
		zOut.close();
	}

}
