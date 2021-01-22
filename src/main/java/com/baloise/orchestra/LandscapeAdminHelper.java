package com.baloise.orchestra;

import static java.lang.String.format;
import static java.util.Collections.singletonMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.ws.BindingProvider;

import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.SubnodeConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import com.baloise.common.FactoryHashMap;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

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
	private Log log;
	private String orchestraHost;
	
	
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
		this.orchestraHost = orchestraHost;
	}
	public LandscapeAdminHelper(String user, String password, URI orchestraServer) {
		LandscapeAdministration dserv = new LandscapeAdministration(Lambda.run(()-> orchestraServer.resolve("/OrchestraRemoteService/LandscapeAdmin/Service?wsdl").toURL())) ;
		port = dserv.getPort(LandscapeAdministrationPort.class);

		BindingProvider prov = (BindingProvider) port;
		prov.getRequestContext().put(BindingProvider.USERNAME_PROPERTY, user);
		prov.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, password);
		log = Log.DEFAULT;
	}
	
	private Map<String, Map<String, String>> map(INIConfiguration ini) {
		FactoryHashMap<String, Map<String, String>> ret = FactoryHashMap.create(()-> new HashMap<String, String>());
		for(String landscapeEntryName : ini.getSections()) {
			SubnodeConfiguration landscapeEntryValues = ini.getSection(landscapeEntryName);
			Iterator<String> keys = landscapeEntryValues.getKeys();
			while (keys.hasNext()) {
				String key = keys.next().toLowerCase();
				Map<String, String> values = ret.get(key);
				if(values.put(key, landscapeEntryValues.getString(key)) != null) {
					log.warn("overwriting " +key);
				}
			}
		}
		return ret;
	}
	
	private Map<String, Map<String, String>> mapJson(String filePath, String jsonPath) throws IOException {
		FactoryHashMap<String, Map<String, String>> ret = FactoryHashMap.create(()-> new HashMap<String, String>());
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
		mapper.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
		
		File jsonFile = new File(filePath);
		checkFileExists(filePath, jsonFile);
		Map<String, Object> results = null;
		
		try {
			results = mapper.readValue(jsonFile, new TypeReference<Map<String, Object>>() { } );
		} catch (Exception e) {
			log.error(e.getMessage() + " while parsing " + filePath);
			throw e;
		}
		
		if(jsonPath !=null) {
			for (String pathElement : jsonPath.split("/")) {
				results = (Map<String, Object>) results.get(pathElement);
				if(results == null) {
					String message = format("JSON path element '%s' not found in %s", pathElement, filePath);
					log.error(message);
					throw new IOException(message);
				}
			}
		}
		
		results.forEach((section,props) -> {
			Map<String, String> values = ret.get(section);
			Map<String, Object> vm = Map.class.isAssignableFrom(props.getClass()) ? (Map) props : singletonMap("VALUE", props);
			vm.forEach((key,value) -> {
				key = key.toLowerCase();
				String stringValue =
						Array.class.isAssignableFrom(value.getClass()) ?
						Arrays.stream((Object[]) value).map(Objects::toString).collect(Collectors.joining(", ")) :
						value.toString();	
				if(values.put(key, stringValue) != null) {
					log.warn("overwriting " +key);
				}
			});
		});
		
		return ret;
	}
	
	private void checkFileExists(String filePath, File jsonFile) throws FileNotFoundException {
		if(!jsonFile.exists()) {
			String message = filePath + " not found";
			log.error(message);
			throw new FileNotFoundException(message);
		}
	}
	
	public void deploy(String scenarioId, Map<String, Map<String, String>> params) throws IOException {
		EmdsEpiDeclBasedataScenarioIdentifier scenario = new EmdsEpiDeclBasedataScenarioIdentifier().withScenario(scenarioId);

		Map<String, EmdsEpiDeclServerLandscapeDataLandscapeInfo> info = getLandscapeInfo(scenario);
		params.forEach((landscapeEntryName , landscapeEntryValues)-> {
			log.info("configuring " + landscapeEntryName);
			storeLandscapeData(scenario, info, landscapeEntryName, landscapeEntryValues);
		});
		log.info("landscapes as json from the orchestra server");
		log.info(getLandscapeAsJson(scenarioId, true));
	}
	
	public Log getLog() {
		return log;
	}
	
	private void storeLandscapeData(EmdsEpiDeclBasedataScenarioIdentifier scenario,
			Map<String, EmdsEpiDeclServerLandscapeDataLandscapeInfo> info, String landscapeEntryName,
			 Map<String, String> landscapeEntryValues) {
		
		EmdsEpiDeclServerLandscapeDataLandscapeInfo theInfo = info.get(landscapeEntryName);
		if(theInfo == null) {
			log.accept(format("WARNING - landscape not found : '%s'", landscapeEntryName));
			return;
		}
		GetLandscapeDataResponse landscapeDataResponse = getLandscapeData(scenario, theInfo);
		
		Map<String, EmdsEpiDeclServerLandscapeDataLandscapeEntryValue> values = landscapeDataResponse.getResult().stream().collect(toMap((e)->{return e.getName().toLowerCase();},identity()));

		landscapeEntryValues.forEach((key,newValue)->{
			EmdsEpiDeclServerLandscapeDataLandscapeEntryValue value = values.get(key);
			if(value!= null) {
				value.setValue(newValue);
			} else {
				log.info(format("WARNING - key not found : '%s'", key));
			}
		});
		
		StoreLandscapeDataRequest parameter = new StoreLandscapeDataRequest();
		parameter.setReference(theInfo.getReference());
		parameter.setScenarioID(theInfo.getScenario());
		parameter.getData().addAll(values.values());
		port.storeLandscapeData(parameter);
	}
	private GetLandscapeDataResponse getLandscapeData(EmdsEpiDeclBasedataScenarioIdentifier scenario,
			EmdsEpiDeclServerLandscapeDataLandscapeInfo landscapeInfo) {
		GetLandscapeDataRequest dataRequest = new GetLandscapeDataRequest().withScenarioID(scenario).withReference(landscapeInfo.getReference());
		GetLandscapeDataResponse landscapeDataResponse = port.getLandscapeData(dataRequest);
		return landscapeDataResponse;
	}
	
	private Map<String, EmdsEpiDeclServerLandscapeDataLandscapeInfo> getLandscapeInfo(EmdsEpiDeclBasedataScenarioIdentifier scenario) {
		GetLandcapeInfoResponse landcapeInfo = port.getLandcapeInfo(new GetLandcapeInfoRequest().withScenario(scenario));
		return landcapeInfo.getResult().stream().collect(Collectors.toMap(EmdsEpiDeclServerLandscapeDataLandscapeInfo::getEntryName,Function.identity()));
	}
	
	
	public LandscapeAdminHelper withLog(Log log) {
		this.log = log;
		return this;
	}
	
	public void deploy(String scenarioId, String landscapeURI) throws IOException {
		String[] tokens = landscapeURI.split(Pattern.quote(File.pathSeparator),2);
		if(tokens[0].endsWith("json")) {
			deploy(scenarioId, mapJson(tokens[0], tokens.length == 2 ? tokens[1] : null));
		} else {
			try {
				deploy(scenarioId, map(new Configurations().ini(tokens[0])));
			} catch (ConfigurationException e) {
				throw new IOException("Could not deploy landscape " + landscapeURI, e);
			}
		}
	}
	
	public String getLandscapeAsJson(String scenarioId, boolean hideEncryptedValue) throws IOException {
		EmdsEpiDeclBasedataScenarioIdentifier scenario = new EmdsEpiDeclBasedataScenarioIdentifier().withScenario(scenarioId);
		Map<String, EmdsEpiDeclServerLandscapeDataLandscapeInfo> info = getLandscapeInfo(scenario);
		Map<String, Map<String,String>> json = new TreeMap<>();
		info.forEach((name,i)->{
			GetLandscapeDataResponse data = getLandscapeData(scenario, i);
			Map<String,String> attributes = new TreeMap<>();
			data.getResult().stream().forEach((entryValue) -> {
				attributes.put(entryValue.getName(), hideEncryptedValue && entryValue.getEncrypted() ? "*************************************" :entryValue.getValue());
			});
			json.put(i.getEntryName(), attributes);
		});
		Map<String, Map<String, Map<String, String>>> fullJson = new TreeMap<>();
		fullJson.put(orchestraHost, json);
		return getPrettyJson(fullJson);
	}
	private String getPrettyJson(Object json) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
	}
	
}