package com.baloise.os.webservice;

import java.io.IOException;

import com.baloise.os.webservice.handler.HTTP;

public class BasicAuthHandler extends AbstractInvocationHandler {

	private String user;
	private String password;

	public BasicAuthHandler(Class<?> clazz, String endPoint, Validate validate, String user, String password) {
		super(clazz, endPoint, validate);
		this.user = user;
		this.password = password;
	}

	protected HTTP.Response postRequest(String xml) throws IOException {
		return HTTP.postBasicAuth(endPoint, user, password, xml);
	}
	
}
