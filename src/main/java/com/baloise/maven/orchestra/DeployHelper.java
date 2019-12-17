package com.baloise.maven.orchestra;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.Properties;
import java.util.function.Consumer;

import javax.xml.ws.BindingProvider;

import emds.epi.decl.server.deployment.deploymentservice.ActivateScenarioRequest;
import emds.epi.decl.server.deployment.deploymentservice.AquireDeploymentTokenRequest;
import emds.epi.decl.server.deployment.deploymentservice.AquireDeploymentTokenResponse;
import emds.epi.decl.server.deployment.deploymentservice.DeActivateScenarioRequest;
import emds.epi.decl.server.deployment.deploymentservice.DeployScenarioCallbackRequest;
import emds.epi.decl.server.deployment.deploymentservice.DeploymentService;
import emds.epi.decl.server.deployment.deploymentservice.DeploymentServicePort;
import emds.epi.decl.server.deployment.deploymentservice.EmdsEpiDeclBasedataScenarioIdentifier;
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

	private URL keepCompilerHappy(URI orchestraServer) {
		try {
			return orchestraServer.resolve("/OrchestraRemoteService/DeploymentService/Service?wsdl").toURL();
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public DeployHelper(String user, String password, URI orchestraServer) {
		DeploymentService dserv = new DeploymentService(keepCompilerHappy(orchestraServer));
		port = dserv.getPort(DeploymentServicePort.class);

		BindingProvider prov = (BindingProvider) port;
		prov.getRequestContext().put(BindingProvider.USERNAME_PROPERTY, user);
		prov.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, password);

	}

	public void deploy(File psc) throws IOException {
		deploy(psc, false);
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
				putdeploy(port::reDeployScenarioCallback, createRedeployRequest(psc));
			} else {
				putdeploy(port::deployScenarioCallback, createDeployRequest(psc));
			}
			if (autostart || wasRunning)
				startScenario(uuid);
		} finally {
			port.freeDeploymentToken(new FreeDeploymentTokenRequest().withToken(token));
		}
	}

	private ReDeployScenarioCallbackRequest createRedeployRequest(File psc) throws IOException {
		return new ReDeployScenarioCallbackRequest().withToken(token).withSerializedScenario(Files.readAllBytes(psc.toPath()));
	}

	private DeployScenarioCallbackRequest createDeployRequest(File psc) throws IOException {
		return new DeployScenarioCallbackRequest().withToken(token).withSerializedScenario(Files.readAllBytes(psc.toPath()));
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
	

	private <T> void putdeploy(Consumer<T> deployOrRedeploy, T request) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	private boolean isStarted(String uuid) {
		return getScenarioInfo(uuid).getResult().getActive();
	}

	private boolean isDeployed(String uuid) {
		return getScenarioInfo(uuid).getResult() != null;
	}

}