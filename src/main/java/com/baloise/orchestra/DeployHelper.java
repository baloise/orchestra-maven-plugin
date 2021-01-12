package com.baloise.orchestra;

import static com.baloise.orchestra.DeployHelper.DeploymentType.Deployment;
import static com.baloise.orchestra.DeployHelper.DeploymentType.Redeployment;
import static java.lang.String.format;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;
import java.util.Optional;
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

	static enum DeploymentType {
		Deployment, Redeployment
	}
	
	private DeploymentServicePort port;
	private EmdsEpiDeclServerDeploymentDataDeploymentToken token;
	private int retryCount = 30;
	private long retryDelayMillies = 1000;
	private Log log;
	private String comment;
	
	
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
	

	public DeployHelper(String user, String password, String orchestraHost) {
		this(user, password, Lambda.run(()->new URI(format("http://%s:8019", orchestraHost))));
	}
	public DeployHelper(String user, String password, URI orchestraServer) {
		DeploymentService dserv = new DeploymentService(Lambda.run(()-> orchestraServer.resolve("/OrchestraRemoteService/DeploymentService/Service?wsdl").toURL())) ;
		port = dserv.getPort(DeploymentServicePort.class);

		BindingProvider prov = (BindingProvider) port;
		prov.getRequestContext().put(BindingProvider.USERNAME_PROPERTY, user);
		prov.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, password);
		log = Log.DEFAULT;
	}

	public String deploy(File psc) throws IOException {
		return deploy(psc, false);
	}
	

	public DeployHelper withRetryCount(int retryCount) {
		this.retryCount = retryCount;
		return this;
	}

<<<<<<< HEAD
	public DeployHelper withRetryDelayMillies(long retryDelayMillies) {
		this.retryDelayMillies = retryDelayMillies;
=======
	public DeployHelper withRetryDeplayMillies(long retryDeplayMillies) {
		this.retryDelayMillies = retryDeplayMillies;
>>>>>>> branch 'master' of https://github.com/baloise/orchestra-maven-plugin.git
		return this;
	}
	public DeployHelper withLog(Log log) {
		this.log = log;
		return this;
	}
	
	public Log getLog() {
		return log;
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
		        log.info(format("waiting for deployment to finish : %s attempts left ", retry));
	        	retry--;
	        	sleep();
			}
	        throw new IOException(format("no success message from orchestra after %s attempts", retryCount));
        } finally {
        	if(res != null) {
        		log.info("DATE                -                   ORIGINATOR (CATEGORY) : DESCRIPTION ");
        		res.stream()
        				.sorted(comparing(EmdsEpiDeclServerDeploymentDataDeploymentInfo::getDate))
        				.map(this::formatInfo)
        				.forEach(log);
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