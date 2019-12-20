package com.baloise.os.webservice;

import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class SimpleWebserviceClient {
	
	static Map<String, Object> cache = new HashMap<>();
	@SuppressWarnings("unchecked")
	public static <T> T createBasicAuth(Class<? extends T> interfaceClass, String endPoint,Validate validate, String user, String password) {
		final String key = key(interfaceClass,endPoint,user,password);
		Object ret = cache.get(key);
		if(ret == null) {
			ret = Proxy.newProxyInstance(SimpleWebserviceClient.class.getClassLoader(), new Class[] { interfaceClass }, new BasicAuthHandler(interfaceClass, endPoint, validate, user, password));
			cache.put(key, ret);
		}
		return (T) ret;
	}
	
	public static <T> T  createBasicAuth(Class<? extends T>  interfaceClass, URI endPoint, String user, String password) {
		return createBasicAuth(interfaceClass, endPoint, user, password, Validate.NONE);
	}
	
	public static <T> T  createBasicAuth(Class<? extends T>  interfaceClass, URI endPoint, String user, String password, Validate validate) {
		return createBasicAuth(interfaceClass, ""+endPoint, validate, user, password);
	}

	@SuppressWarnings("unchecked")
	public static <T> T  createSAML(Class<? extends T>  interfaceClass, String endPoint, Validate validate, String headers) {
		final String key = key(interfaceClass,endPoint,headers);
		Object ret = cache.get(key);
		if(ret == null) {
			ret = Proxy.newProxyInstance(SimpleWebserviceClient.class.getClassLoader(), new Class[] { interfaceClass }, new SAMLHandler(interfaceClass, endPoint, validate, headers));
			cache.put(key, ret);
		}
		return (T) ret;
	}
	
	public static <T> T  createSAML(Class<? extends T>  interfaceClass, URI endPoint, String token) {
		return createSAML(interfaceClass, endPoint, token, Validate.NONE);
	}
	
	public static <T> T  createSAML(Class<? extends T>  interfaceClass, URI endPoint, String token, Validate validate) {
		return createSAML(interfaceClass, ""+endPoint, validate, token);
	}

	private static String key(Object ... objects) {
		StringBuilder sb = new StringBuilder();
		for (Object object : objects) {
			sb.append(object);
			sb.append("#/:-");
		}
		return sb.toString();
	}
	
}
