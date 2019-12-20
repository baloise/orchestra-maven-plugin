package com.baloise.os.webservice;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

import javax.xml.bind.JAXBException;
import javax.xml.bind.PropertyException;

import org.xml.sax.SAXException;

import com.baloise.os.webservice.handler.HTTP;

public class SAMLHandler extends AbstractInvocationHandler {

	private String headers;

	public SAMLHandler(Class<?> clazz, String endPoint, Validate validate, String headers) {
		super(clazz, endPoint, validate);
		this.headers = headers;
	}

	protected HTTP.Response  postRequest(String xml) throws IOException {
		return HTTP.post(endPoint, samlify(xml));
	}
	
	@Override
	protected Object unMarshal(String input) throws JAXBException, PropertyException, UnsupportedEncodingException, MalformedURLException, SAXException {
		return super.unMarshal(deSamlify(input));
	}
	
	private String deSamlify(String body) {
		return body.replaceFirst("<\\w+:Header>[\\w\\W]*?</\\w+:Header>", "");
	}

	private String samlify(String xml) {
		return xml.replaceFirst("<(\\w+):Body>", "<$1:Header>"+headers+"</$1:Header>\n    <$1:Body>");
	}
}
