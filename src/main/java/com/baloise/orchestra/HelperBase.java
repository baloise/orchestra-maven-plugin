package com.baloise.orchestra;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.xml.ws.BindingProvider;


abstract class HelperBase<T> {

	static enum DeploymentType {
		Deployment, Redeployment
	}
	
	T port;
	private Log log;
	String orchestraHost;
	String protocol;
	
	@FunctionalInterface
	private static interface Lambda<T> {
	    T call() throws Exception;
	    static <T> T run(Lambda<T> p) {
	    	try {
	    		return p.call();
	    	} catch (Exception e) {
	    		throw new IllegalArgumentException(e);
	    	}
	    }
	}
	

	static List<URL> parseURL(String u) throws MalformedURLException {
		List<URL> ret = new ArrayList<>();
		for(String host : u.split(",")) {
			try {
				ret.add(new URL(host));
			} catch (MalformedURLException e) {
				ret.add(new URL(format("https://%s:8443", host)));
			}
		}
		return ret;
	}
	
	
	public HelperBase(String user, String password, String orchestraHost) {
		this(
				user, 
				password, 
				Lambda.run(()-> parseURL(orchestraHost)));
	}
	
	public HelperBase(String user, String password, URL orchestraServer) {
		this(user, password, singletonList(orchestraServer));
		
	}
	
	
	abstract String getWsdlPath() ;
	//TODO refactor
	abstract T getPort(URL wsdlURL);
	
	public HelperBase(String user, String password, List<URL> orchestraServers) {
		log = Log.DEFAULT;
		Collections.shuffle(orchestraServers);
		for (URL orchestraServer : orchestraServers) {
			try {
				orchestraHost = orchestraServer.getHost();
				protocol = orchestraServer.getProtocol().toLowerCase();
				URL wsdlURL = orchestraServer.toURI().resolve(getWsdlPath()).toURL();
				log.debug("trying to establish connection to "+ wsdlURL);
				port = getPort(wsdlURL);
				BindingProvider prov = (BindingProvider) port;
				prov.getRequestContext().put(BindingProvider.USERNAME_PROPERTY, user);
				prov.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, password);
				
			} catch (MalformedURLException | URISyntaxException e) {
				log.info(format("Could not form a valid server URL for %s : %s", orchestraServer, e.getMessage()));
			} catch (Exception e) {
				log.info(format("Server %s not reachable : %s", orchestraServer, e.getMessage()));
			}
			if(port != null) {
				getLog().info(format("orchestra host is %s://%s",protocol, orchestraHost));
				break;
			}
		}
		if(port == null) {
			throw new IllegalArgumentException(format("Could not reach any of the following servers: %s", orchestraServers));
		}
	}


	public <H extends HelperBase<T>> H withLog(Log log) {
		this.log = log;
		return (H) this;
	}
	
	public Log getLog() {
		return log;
	}


}