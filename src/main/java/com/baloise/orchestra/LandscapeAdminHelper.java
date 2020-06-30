package com.baloise.orchestra;

import static java.lang.String.format;

import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.ws.BindingProvider;

import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.SubnodeConfiguration;

import emds.epi.decl.server.landscape.landscapeadministration.EmdsEpiDeclBasedataScenarioIdentifier;
import emds.epi.decl.server.landscape.landscapeadministration.EmdsEpiDeclServerLandscapeDataLandscapeEntryValue;
import emds.epi.decl.server.landscape.landscapeadministration.EmdsEpiDeclServerLandscapeDataLandscapeInfo;
import emds.epi.decl.server.landscape.landscapeadministration.GetLandcapeInfoRequest;
import emds.epi.decl.server.landscape.landscapeadministration.GetLandcapeInfoResponse;
import emds.epi.decl.server.landscape.landscapeadministration.GetLandscapeDataRequest;
import emds.epi.decl.server.landscape.landscapeadministration.GetLandscapeDataResponse;
import emds.epi.decl.server.landscape.landscapeadministration.LandscapeAdministration;
import emds.epi.decl.server.landscape.landscapeadministration.LandscapeAdministrationPort;
import emds.epi.decl.server.landscape.landscapeadministration.StoreLandscapeDataRequest;

public class LandscapeAdminHelper {

	private LandscapeAdministrationPort port;
	private Consumer<Object> log;
	
	
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
	

	public LandscapeAdminHelper(String user, String password, String orchestraHost) {
		this(user, password, Lambda.run(()->new URI(format("http://%s:8019", orchestraHost))));
	}
	public LandscapeAdminHelper(String user, String password, URI orchestraServer) {
		LandscapeAdministration dserv = new LandscapeAdministration(Lambda.run(()-> orchestraServer.resolve("/OrchestraRemoteService/LandscapeAdmin/Service?wsdl").toURL())) ;
		port = dserv.getPort(LandscapeAdministrationPort.class);

		BindingProvider prov = (BindingProvider) port;
		prov.getRequestContext().put(BindingProvider.USERNAME_PROPERTY, user);
		prov.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, password);
		log = System.out::println;
	}
	
	public void deploy(String scenarioId, INIConfiguration ini) {
		EmdsEpiDeclBasedataScenarioIdentifier scenario = new EmdsEpiDeclBasedataScenarioIdentifier().withScenario(scenarioId);
		
		Map<String, EmdsEpiDeclServerLandscapeDataLandscapeInfo> info = getLandscapeInfo(scenario);
		
		for( String landscapeEntryName :ini.getSections()) {
			log.accept("configuring " + landscapeEntryName);
			storeLandscapeData(scenario, info, landscapeEntryName, ini.getSection(landscapeEntryName));
		}
	}
	
	
	public Consumer<Object> getLog() {
		return log;
	}
	
	private void storeLandscapeData(EmdsEpiDeclBasedataScenarioIdentifier scenario,
			Map<String, EmdsEpiDeclServerLandscapeDataLandscapeInfo> info, String landscapeEntryName,
			SubnodeConfiguration landscapeEntryValues) {
		
		EmdsEpiDeclServerLandscapeDataLandscapeInfo theInfo = info.get(landscapeEntryName);
		GetLandscapeDataRequest dataRequest = new GetLandscapeDataRequest().withScenarioID(scenario).withReference(theInfo.getReference());
		GetLandscapeDataResponse landscapeDataResponse = port.getLandscapeData(dataRequest);
		Map<String, EmdsEpiDeclServerLandscapeDataLandscapeEntryValue> values = landscapeDataResponse.getResult().stream().collect(Collectors.toMap(EmdsEpiDeclServerLandscapeDataLandscapeEntryValue::getName,Function.identity()));
		
		Iterator<String> keys = landscapeEntryValues.getKeys();
		while (keys.hasNext()) {
			String key = keys.next();
			EmdsEpiDeclServerLandscapeDataLandscapeEntryValue value = values.get(key);
			if(value!= null) {
				value.setValue(landscapeEntryValues.getString(key));
			} else {
				log.accept(format("WARNING - key not found : '%s'", key));
			}
		}
		
		StoreLandscapeDataRequest parameter = new StoreLandscapeDataRequest();
		parameter.setReference(theInfo.getReference());
		parameter.setScenarioID(theInfo.getScenario());
		parameter.getData().addAll(values.values());
		port.storeLandscapeData(parameter);
	}
	
	private Map<String, EmdsEpiDeclServerLandscapeDataLandscapeInfo> getLandscapeInfo(EmdsEpiDeclBasedataScenarioIdentifier scenario) {
		GetLandcapeInfoResponse landcapeInfo = port.getLandcapeInfo(new GetLandcapeInfoRequest().withScenario(scenario));
		return landcapeInfo.getResult().stream().collect(Collectors.toMap(EmdsEpiDeclServerLandscapeDataLandscapeInfo::getEntryName,Function.identity()));
	}
	
	
	public LandscapeAdminHelper withLog(Consumer<Object> log) {
		this.log = log;
		return this;
	}
	
}