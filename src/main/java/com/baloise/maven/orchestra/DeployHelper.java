package com.baloise.maven.orchestra;

import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import javax.xml.ws.BindingProvider;

import emds.epi.decl.server.deployment.deploymentservice.ActivateScenarioRequest;
import emds.epi.decl.server.deployment.deploymentservice.AquireDeploymentTokenRequest;
import emds.epi.decl.server.deployment.deploymentservice.AquireDeploymentTokenResponse;
import emds.epi.decl.server.deployment.deploymentservice.DeActivateScenarioRequest;
import emds.epi.decl.server.deployment.deploymentservice.DeployScenarioCallbackRequest;
import emds.epi.decl.server.deployment.deploymentservice.DeploymentService;
import emds.epi.decl.server.deployment.deploymentservice.DeploymentServicePort;
import emds.epi.decl.server.deployment.deploymentservice.EmdsEpiDeclBasedataScenarioIdentifier;
import emds.epi.decl.server.deployment.deploymentservice.EmdsEpiDeclServerDeploymentDataDeploymentInfo;
import emds.epi.decl.server.deployment.deploymentservice.EmdsEpiDeclServerDeploymentDataDeploymentToken;
import emds.epi.decl.server.deployment.deploymentservice.FreeDeploymentTokenRequest;
import emds.epi.decl.server.deployment.deploymentservice.GetDeploymentInfoRequest;
import emds.epi.decl.server.deployment.deploymentservice.GetDeploymentInfoResponse;
import emds.epi.decl.server.deployment.deploymentservice.GetScenarioInfoRequest;
import emds.epi.decl.server.deployment.deploymentservice.GetScenarioInfoResponse;
import emds.epi.decl.server.deployment.deploymentservice.ReDeployScenarioCallbackRequest;

public class DeployHelper {

	private DeploymentServicePort port;
	private EmdsEpiDeclServerDeploymentDataDeploymentToken token;
	private int retryCount = 30;
	private long retryDeplayMillies = 1000;
	
	
	@FunctionalInterface
	private static interface Lambda<T> {
	    T call() throws Exception;
	}
	
	private static <T> T keepCompilerHappy(Lambda<T> p) {
		try {
			return p.call();
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

	public DeployHelper(String user, String password, String orchestraHost) {
		this(user, password, keepCompilerHappy(()->new URI(format("http://%s:8019", orchestraHost))));
	}
	public DeployHelper(String user, String password, URI orchestraServer) {
		DeploymentService dserv = new DeploymentService(keepCompilerHappy(()-> orchestraServer.resolve("/OrchestraRemoteService/DeploymentService/Service?wsdl").toURL())) ;
		port = dserv.getPort(DeploymentServicePort.class);

		BindingProvider prov = (BindingProvider) port;
		prov.getRequestContext().put(BindingProvider.USERNAME_PROPERTY, user);
		prov.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, password);

	}

	public void deploy(File psc) throws IOException {
		deploy(psc, false);
	}
	

	public DeployHelper withRetryCount(int retryCount) {
		this.retryCount = retryCount;
		return this;
	}

	public DeployHelper withRetryDeplayMillies(long retryDeplayMillies) {
		this.retryDeplayMillies = retryDeplayMillies;
		return this;
	}

	public void deploy(File psc, boolean autostart) throws IOException {
		AquireDeploymentTokenResponse deploymentToken = port.aquireDeploymentToken(new AquireDeploymentTokenRequest());
		token = deploymentToken.getResult();
		Properties properties = PSCHelper.getScenarioProperties(psc);
		String uuid = properties.getProperty("UUID");
		try {
			boolean wasRunning = false;
			if (isDeployed(uuid)) {
				wasRunning = isStarted(uuid);
				if (wasRunning)
					stopScenario(uuid);
				requestRedeploy(psc);
			} else {
				requestDeploy(psc);
			}
			if (autostart || wasRunning) 
				startScenario(uuid);
		} finally {
			port.freeDeploymentToken(new FreeDeploymentTokenRequest().withToken(token));
		}
	}


	private void stopScenario(String uuid) {
		port.deActivateScenario(new DeActivateScenarioRequest().withToken(token).withScenarioID(new EmdsEpiDeclBasedataScenarioIdentifier().withScenario(uuid)));
	}

	private void startScenario(String uuid) {
	   port.activateScenario(new ActivateScenarioRequest().withToken(token).withScenarioID(new EmdsEpiDeclBasedataScenarioIdentifier().withScenario(uuid)));
	}

	private GetDeploymentInfoResponse getDeploymentInfo() {
		return port.getDeploymentInfo(new GetDeploymentInfoRequest().withToken(token));
	}
	
	private GetScenarioInfoResponse getScenarioInfo(String uuid) {
		return port.getScenarioInfo(new GetScenarioInfoRequest()
				.withScenarioID(new EmdsEpiDeclBasedataScenarioIdentifier().withScenario(uuid))
				);
	}
	

	private void requestDeploy(File psc) throws IOException {
		port.deployScenarioCallback(
				new DeployScenarioCallbackRequest()
					.withToken(token)
					.withSerializedScenario(Files.readAllBytes(psc.toPath()))
				);
		waitForDeploy();
		
	}
	
	private void requestRedeploy(File psc) throws IOException {
		port.reDeployScenarioCallback(
				new ReDeployScenarioCallbackRequest()
				.withToken(token)
				.withSerializedScenario(Files.readAllBytes(psc.toPath()))
				);
		waitForDeploy();
	}
	

	private void waitForDeploy() throws IOException {
        int retry = retryCount;
		while(retry>0) {
	        GetDeploymentInfoResponse info = getDeploymentInfo();
	        Set<String> descs = info.getResult().stream().map(EmdsEpiDeclServerDeploymentDataDeploymentInfo::getDescription).collect(toSet());
	        if(descs.contains("Redeployment finished."))
	        	return;
	        if(descs.stream().filter(i->i.contains("currently not allowed")).findAny().isPresent()) {
	        	System.out.println(format("waiting for deployment to finish : %s attempts left ", retry));
	        	retry--;
	        	try {
	        		Thread.sleep(retryDeplayMillies);
	        	} catch (InterruptedException wakeUp) {
	        	}
	        }
	        Optional<String> failure = descs.stream().filter(i->i.startsWith("Redeployment failed") && !i.endsWith(":  null")).findAny();
			if(failure.isPresent()) {
	        	throw new IOException(failure.get());	        	
	        }
		}
		throw new IOException(format("no success message from orchestra after %s attempts", retryCount));
	}

	private boolean isStarted(String uuid) {
		return getScenarioInfo(uuid).getResult().getActive();
	}

	private boolean isDeployed(String uuid) {
		return getScenarioInfo(uuid).getResult() != null;
	}

}