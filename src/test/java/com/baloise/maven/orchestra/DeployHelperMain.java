package com.baloise.maven.orchestra;

import java.io.File;

public class DeployHelperMain {
	public static void main(String[] args) throws Exception {
		File psc = new File("C:\\Users\\b028178\\Downloads\\vm_aperture_to_vm-d9be335f8734cfb304d1a799d3d6d9e823007ff3.psc");
		DeployHelper deployHelper = new DeployHelper("Deploy", "deploy123", "svw-orchi001.balgroupit.com").withRetryCount(6);
		//DeployHelper deployHelper = new DeployHelper("Deploy", "deploy123", "sandbox-orchestra.balgroupit.com").withRetryCount(6);
		//DeployHelper deployHelper = new DeployHelper("Deploy", "deploy123", "svw-orcht001.balgroupit.com");
		String uuid = PSCHelper.getScenarioProperties(psc).getProperty("UUID");
		System.out.println(uuid);
		System.out.println(deployHelper.isDeployed(uuid));
		deployHelper.deploy(psc);
	}
}
