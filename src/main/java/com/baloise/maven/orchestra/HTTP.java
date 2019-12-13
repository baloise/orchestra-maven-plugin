package com.baloise.maven.orchestra;

import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.codehaus.plexus.util.IOUtil;

public final class HTTP {
	@FunctionalInterface
	public static interface Entity {

	    void writeTo(OutputStream out) throws IOException;
	   
	    default Entity wrap(Entity before,  Entity after) {
	    	return (out) -> {
	    		before.writeTo(out);
	    		writeTo(out);
	    		after.writeTo(out);
	    	};
	    }
	}
	static String post(String url, String auth, Entity entity) throws IOException {
		HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
		con.setRequestProperty("Authorization", auth);
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "text/xml;charset=UTF-8");
		con.setDoOutput(true);
		try(OutputStream out = con.getOutputStream()) {			
			entity.writeTo(out);
		}
		int code = con.getResponseCode();
		String resp;
		try(InputStream out = con.getInputStream()) {			
			resp = IOUtil.toString(con.getInputStream());
		}
		if(code >=300) 
			throw new IOException(format("response code %s : " + resp, code));
		return resp;
	}

}
