package com.baloise.orchestra;

import static java.lang.String.format;
import static java.util.Collections.singletonList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;

import emds.epi.decl.server.deployment.deploymentservice.DeploymentService;
import emds.epi.decl.server.deployment.deploymentservice.DeploymentServicePort;


abstract class HelperBase<T> {

	static enum DeploymentType {
		Deployment, Redeployment
	}
	
	T port;
	private Log log;
	String orchestraHost;
	String protocol;
	private Class<T> servicePortClass;
	private Class<T> serviceClass;
	
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
	
	
	public String getWsdlPath() {
		return getServicePath()+"?wsdl";
	}
	
	abstract String getServicePath();


	public T getPort(URL wsdlURL) {
		try {
			Service service = (Service) serviceClass.getConstructor(URL.class).newInstance(wsdlURL);
			Iterator<QName> ports = service.getPorts();
			
			while(ports.hasNext()) {
				QName n = ports.next();
				port = (T) service.getPort(n, servicePortClass);
				if(port.toString().toLowerCase().contains(protocol)) return port;
			}
			
			throw new IllegalStateException("no port found with protocol: "+ protocol);
			
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public HelperBase(String user, String password, List<URL> orchestraServers) {
		log = Log.DEFAULT;
		loadClasses();
		
		Collections.shuffle(orchestraServers);
		for (URL orchestraServer : orchestraServers) {
			try {
				orchestraHost = orchestraServer.getHost();
				protocol = orchestraServer.getProtocol().toLowerCase();
				URL wsdlURL = getWsdlUrl(orchestraServer);
				log.debug("trying to establish connection to "+ wsdlURL);
				port = getPort(wsdlURL);
				BindingProvider prov = (BindingProvider) port;
				prov.getRequestContext().put(BindingProvider.USERNAME_PROPERTY, user);
				prov.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, password);
				if(getBooleanProperty("com.baloise.maven.orchestra.overrideWsdlEndpoint")) {
					prov.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, getServiceUrl(orchestraServer).toString());
				}
				Object endpoint = prov.getRequestContext().get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
				log.debug("endpoint:" + endpoint);
				
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


	URL getWsdlUrl(URL orchestraServer) throws MalformedURLException, URISyntaxException {
		return orchestraServer.toURI().resolve(getWsdlPath()).toURL();
	}
	
	URL getServiceUrl(URL orchestraServer) throws MalformedURLException, URISyntaxException {
		return orchestraServer.toURI().resolve(getServicePath()).toURL();
	}


	private boolean getBooleanProperty(String name) {
		return Boolean.valueOf(System.getProperty(name, "false"));
	}


	private void loadClasses() {
		servicePortClass = (Class<T>)
				   ((ParameterizedType)getClass().getGenericSuperclass())
				      .getActualTypeArguments()[0];
		String serviceClassname = servicePortClass.getName();
		serviceClassname = serviceClassname.substring(0, serviceClassname.length()-4);
		try {
			serviceClass = (Class<T>) Class.forName(serviceClassname);
		} catch (ClassNotFoundException e1) {
			throw new IllegalArgumentException("Service class "+serviceClassname+" not found",e1);
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