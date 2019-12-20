package com.baloise.os.webservice.handler;

import java.io.ByteArrayInputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.baloise.webservice.Envelope;
import com.baloise.webservice.Fault;


public class FaultHandler {
	
	private JAXBContext jaxbContext;

	public FaultHandler(Class<?>[] clazzContext) {
		try {
			jaxbContext = JAXBContext.newInstance(clazzContext);
		} catch (JAXBException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public void handleFault(String responseXML) throws Exception {
		Unmarshaller jaxbMarshaller = jaxbContext.createUnmarshaller();
		JAXBElement<Envelope> jb = (JAXBElement<Envelope>) jaxbMarshaller.unmarshal(new ByteArrayInputStream(responseXML.getBytes("UTF-8")));
		JAXBElement<Fault> ret = (JAXBElement<Fault>) jb.getValue().getBody().getAny().iterator().next();
		final Fault fault = ret.getValue();
		throw new Exception(fault.getFaultstring());
	}

}
