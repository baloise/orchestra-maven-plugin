package com.baloise.orchestra;

import static java.lang.String.format;

import java.net.URI;
import java.util.function.Consumer;

import javax.xml.ws.BindingProvider;

import emds.epi.decl.server.landscape.landscapeadministration.EmdsEpiDeclBasedataScenarioIdentifier;
import emds.epi.decl.server.landscape.landscapeadministration.EmdsEpiDeclServerLandscapeDataLandscapeEntryValue;
import emds.epi.decl.server.landscape.landscapeadministration.EmdsEpiDeclServerLandscapeDataLandscapeInfo;
import emds.epi.decl.server.landscape.landscapeadministration.GetLandcapeInfoRequest;
import emds.epi.decl.server.landscape.landscapeadministration.GetLandcapeInfoResponse;
import emds.epi.decl.server.landscape.landscapeadministration.GetLandscapeDataRequest;
import emds.epi.decl.server.landscape.landscapeadministration.GetLandscapeDataResponse;
import emds.epi.decl.server.landscape.landscapeadministration.LandscapeAdministration;
import emds.epi.decl.server.landscape.landscapeadministration.LandscapeAdministrationPort;

public class LandscapeAdminHelper {

	private LandscapeAdministrationPort port;
	//private EmdsEpiDeclServerDeploymentDataDeploymentToken token;
	private int retryCount = 30;
	private long retryDeplayMillies = 1000;
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
	
	private EmdsEpiDeclServerLandscapeDataLandscapeEntryValue info2value(EmdsEpiDeclServerLandscapeDataLandscapeInfo info) {
		EmdsEpiDeclServerLandscapeDataLandscapeEntryValue value = new EmdsEpiDeclServerLandscapeDataLandscapeEntryValue();
		value.setDescription(info.getEntryDescription());
		value.setName(info.getEntryName());
//		value.setType(info.get);
		return value;
	}
	public void foo() {
		GetLandcapeInfoRequest request = new GetLandcapeInfoRequest();
		EmdsEpiDeclBasedataScenarioIdentifier scenario = new EmdsEpiDeclBasedataScenarioIdentifier().withScenario("0246f9e6-b338-4602-955a-7b4d74f76ce9");
		request.setScenario(scenario);
		GetLandcapeInfoResponse landcapeInfo = port.getLandcapeInfo(request);
		System.out.println(landcapeInfo);
		landcapeInfo.getResult().stream().forEach(info -> System.out.println(info));
		
//		GetLandscapeDataRequest dataRequest = new GetLandscapeDataRequest();
//		dataRequest.setScenarioID(scenario);
//		dataRequest.setRefence("");
//		GetLandscapeDataResponse landscapeDataResponse = port.getLandscapeData(dataRequest);
		
//		StoreLandscapeDataRequest touchParameter = new StoreLandscapeDataRequest();
//		EmdsEpiDeclServerLandscapeDataLandscapeEntryValue value = new EmdsEpiDeclServerLandscapeDataLandscapeEntryValue();
//		value.set
//		touchParameter.getData().add(value );
//		port.storeLandscapeData(touchParameter);
	}
	
	
}