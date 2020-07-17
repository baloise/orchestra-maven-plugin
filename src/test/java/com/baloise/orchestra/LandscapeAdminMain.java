package com.baloise.orchestra;

public class LandscapeAdminMain {

	public static void main(String[] args) throws Exception {

		LandscapeAdminHelper lha = new LandscapeAdminHelper("Deploy", "deploy123", "test-orchestra.balgroupit.com");
		//lha.deploy("0246f9e6-b338-4602-955a-7b4d74f76ce9", "C:\\Users\\Public\\dev\\git\\vm_aperture_to_vm\\landscape\\test.ini");
		lha.deploy("34dd0e31-af39-4ec8-b4d3-216d276a7163", "C:\\Dev\\orchestra\\scenarios\\4.7.4.5\\vm_to_mailquota\\landscape\\landscape.json;test");
	}

} 