package com.baloise.orchestra;

import static com.baloise.orchestra.DeployHelper.DeploymentType.Deployment;
import static com.baloise.orchestra.DeployHelper.DeploymentType.Redeployment;
import static java.lang.String.format;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.xml.namespace.QName;

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

public class DeployHelper extends HelperBase<DeploymentServicePort> {

	public DeployHelper(String user, String password, List<URL> orchestraServers) {
		super(user, password, orchestraServers);
	}

	public DeployHelper(String user, String password, String orchestraHost) {
		super(user, password, orchestraHost);
	}

	public DeployHelper(String user, String password, URL orchestraServer) {
		super(user, password, orchestraServer);
	}

	@Override
	public DeployHelper withLog(Log log) {
		return super.withLog(log);
	}
	
	@Override
	String getWsdlPath() {
		return "/OrchestraRemoteService/DeploymentService/Service?wsdl";
	}
	
	@Override
	DeploymentServicePort getPort(URL wsdlURL) {
		//TODO refactor
		DeploymentService service = new DeploymentService(wsdlURL);
		Iterator<QName> ports = service.getPorts();
		
		while(ports.hasNext()) {
			QName n = ports.next();
			port = service.getPort(n, DeploymentServicePort.class);
			if(port.toString().toLowerCase().contains(protocol)) return port;
		}
		
		throw new IllegalStateException("no port found with protocol: "+ protocol);
	}
	
	static enum DeploymentType {
		Deployment, Redeployment
	}
	
	private EmdsEpiDeclServerDeploymentDataDeploymentToken token;
	private int retryCount = 30;
	private long retryDelayMillies = 1000;
	private String comment;
	
	public String deploy(File psc) throws IOException {
		return deploy(psc, false);
	}

	public DeployHelper withRetryCount(int retryCount) {
		this.retryCount = retryCount;
		return this;
	}

	public DeployHelper withRetryDelayMillies(long retryDelayMillies) {
		this.retryDelayMillies = retryDelayMillies;
		return this;
	}

	public String deploy(File psc, boolean autostart) throws IOException {
		AquireDeploymentTokenResponse deploymentToken = port.aquireDeploymentToken(new AquireDeploymentTokenRequest());
		token = deploymentToken.getResult();
		String uuid = new PSCHelper().getScenarioId(psc);
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
		return uuid;
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
					.withComment(comment !=null? comment : format("deployed %s", new Date()))
				);
		waitFor(Deployment);
	}
	
	private void requestRedeploy(File psc) throws IOException {
		port.reDeployScenarioCallback(
				new ReDeployScenarioCallbackRequest()
				.withToken(token)
				.withSerializedScenario(Files.readAllBytes(psc.toPath()))
				.withComment(comment !=null? comment : format("redeployed %s", new Date()))
				);
		waitFor(Redeployment);
	}
	
	private void sleep() {
		try {
			Thread.sleep(retryDelayMillies);
		} catch (InterruptedException wakeUp) {
		}
	}
	
	static boolean isSuccess(String description, String message) {
		if(description.equals(message+" finished.")) return true;
		if(description.equals(message+" finished without errors")) return true;
		return false;
	}

	private void waitFor(DeploymentType deploymentType) throws IOException {
        int retry = retryCount;
        List<EmdsEpiDeclServerDeploymentDataDeploymentInfo> res = null;
        try {
	        while(retry>0) {
		        GetDeploymentInfoResponse info = getDeploymentInfo();
		        res = info.getResult();
		        Set<String> descs = res.stream().map(EmdsEpiDeclServerDeploymentDataDeploymentInfo::getDescription).collect(toSet());
		        Optional<String> failure = descs.stream().filter(i->i.startsWith(deploymentType.name()+" failed") || i.startsWith("Deployment is currently not possible")).findAny();
		        if(failure.isPresent()) {
		        	throw new IOException(failure.get());	        	
		        }
		        if(descs.stream().filter(d -> isSuccess(d, deploymentType.name())).findAny().isPresent())
		        	return;
		        getLog().info(format("waiting for deployment to finish : %s attempts left ", retry));
	        	retry--;
	        	sleep();
			}
	        throw new IOException(format("no success message from orchestra after %s attempts", retryCount));
        } finally {
        	if(res != null) {
        		getLog().info("DATE                -                   ORIGINATOR (CATEGORY) : DESCRIPTION ");
        		res.stream()
        				.sorted(comparing(EmdsEpiDeclServerDeploymentDataDeploymentInfo::getDate))
        				.map(this::formatInfo)
        				.forEach(getLog());
        	}
		}
	}
	
	private String formatInfo(EmdsEpiDeclServerDeploymentDataDeploymentInfo info) {
		return format("%s - %35s (%s) : %s", info.getDate(), info.getOriginator(), info.getCategory(), info.getDescription());
	}
	
	private boolean isStarted(String uuid) {
		return getScenarioInfo(uuid).getResult().getActive();
	}

	boolean isDeployed(String uuid) {
		return getScenarioInfo(uuid).getResult() != null;
	}
	
	public DeployHelper withComment(String comment) {
		this.comment = comment;
		return this;
	}

}