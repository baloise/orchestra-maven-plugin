package com.baloise.maven.orchestra;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import static com.baloise.maven.orchestra.HTTP.*;


public class OrchestraClient {

	public final String url;
	private final String auth;
	private String token;
	private String nodeID;
	private String tokenXML;

	public OrchestraClient(URL url, String usr, String pwd) {
		this.url = url +"/DeploymentService/Service";
		auth = "Basic " + Base64.getEncoder().encodeToString((usr+":"+pwd).getBytes());
	}

	void aquireDeploymentToken() throws IOException {
		Element result = orchestraSoap("aquireDeploymentToken");
		token = result.getElementsByTagName("emds:tokenData").item(0).getTextContent();
		nodeID = result.getElementsByTagName("emds:nodeID").item(0).getTextContent();
		tokenXML = "<emds:token>" + "<emds:tokenData>" + token + "</emds:tokenData>" + "<emds:nodeID>" + nodeID
				+ "</emds:nodeID>" + "</emds:token>";
		System.out.println(token);
	}

	void freeDeploymentToken() throws IOException {
		orchestraSoap("freeDeploymentToken", tokenXML);
		System.out.println(token + " freed");
	}
	
	void getDeploymentInfo() throws IOException {
		orchestraSoap("getDeploymentInfo", tokenXML);
	}

	Element orchestraSoap(String action) throws IOException {
		return orchestraSoap(action, (out) -> {});
	}

	Element orchestraSoap(String action, String entity) throws IOException {
		return orchestraSoap(action, (out) -> out.write((entity).getBytes()));
	}
	
	public void deployScenario(Path psc) throws IOException {
		try {
			aquireDeploymentToken();
			deployScenarioCallback(psc);
			getDeploymentInfo();
		} finally {
			freeDeploymentToken();
		}
	}
	Element deployScenarioCallback(Path psc) throws IOException {
		return orchestraSoap("deployScenarioCallback", (out)->  {
			out.write(( 
					"<emds:deployScenarioCallback.Request>" + 
					tokenXML + 
					"<emds:serializedScenario>").getBytes());
			
			Files.copy(psc, Base64.getEncoder().wrap(out));
	
			out.write(( 
					"</emds:serializedScenario>" + 
					"<emds:comment>jenkins was here</emds:comment>" + 
					"<emds:groups>" + 
					"</emds:groups>").getBytes());
		});
	}
	
	Element orchestraSoap(String action, Entity entity) throws IOException {
		Entity entityWrapper = entity.wrap((out) -> {
								out.write(("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:emds=\"emds:epi:decl:server:deployment:DeploymentService\">" + 
										"<soapenv:Header/>" + 
										"<soapenv:Body>" + 
										"	<emds:"+action+".Request>").getBytes());
							
								},
					(out) -> {
						out.write(("	</emds:"+action+".Request>" + 
								"</soapenv:Body>" + 
								"</soapenv:Envelope>").getBytes());
					}
		);
		
	   return parse(post(url, auth,entityWrapper));

	}

	Element parse(String resp) throws IOException {
		System.out.println(resp);
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new InputSource( new StringReader( resp ) ));
					
			doc.getDocumentElement().normalize();
			
			return (Element) doc.getDocumentElement()
				.getElementsByTagName("SOAP-ENV:Body").item(0)
				.getChildNodes().item(0);
			
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

}
