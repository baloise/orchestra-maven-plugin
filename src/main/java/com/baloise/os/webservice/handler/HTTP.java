package com.baloise.os.webservice.handler;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.Map;

import org.codehaus.plexus.util.IOUtil;

public final class HTTP {

	public static class Response {
		public Response(String statusText, int statusCode, String body) {
			this.body = body;
			this.statusText = statusText;
			this.status = statusCode;
		}

		public String getBody() {
			return body;
		}
		public String getStatusText() {
			return statusText;
		}
		public int getStatus() {
			return status;
		}

		public final String body;
		public final String statusText;
		public final int status;
	}

	public static Response post(String url, String entity) throws IOException {
		return post(url, entity, emptyMap());
	}

	public static Response postBasicAuth(String url, String user, String password, String entity) throws IOException {
		return post(url, entity, singletonMap("Authorization", "Basic "+Base64.getEncoder().encodeToString((user+":"+password).getBytes())));
	}

	public static Response post(String url, String entity, Map<String, String> headers) throws IOException {
		HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
		con.setRequestMethod("POST");
		if(!headers.containsKey("Content-Type"))
			con.setRequestProperty("Content-Type", "text/xml;charset=UTF-8");
		headers.forEach((key,value)->con.setRequestProperty(key, value));
		con.setDoOutput(true);
		try (OutputStream out = con.getOutputStream()) {
			out.write(entity.getBytes());
		}
		try (InputStream out = con.getInputStream()) {
			return new Response(con.getResponseMessage(), con.getResponseCode(), IOUtil.toString(con.getInputStream()));
		}

	}

}
