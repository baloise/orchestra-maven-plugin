package com.baloise.os.webservice;

import static java.lang.String.format;
import static java.util.Arrays.asList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import org.xml.sax.SAXException;

import com.baloise.os.webservice.handler.FaultHandler;
import com.baloise.os.webservice.handler.HTTP;
import com.baloise.os.webservice.handler.ValidationHandler;
import com.baloise.webservice.Body;
import com.baloise.webservice.Envelope;
import com.baloise.webservice.ObjectFactory;

public abstract class AbstractInvocationHandler implements InvocationHandler {

	private final Class<?> clazz;
	final ValidationHandler validator;
	final ReflectionHelper reflectionHelper = new ReflectionHelper();
	Class<?>[] lazyClazzContext;
	FaultHandler lazyFaultHandler;
	private JAXBContext lazyContext;
	protected String endPoint;
	private final  Validate validate;
	final boolean debugRequest = System.getProperty("SimpleWebserviceClient.debug.request") != null;
	final boolean debugResponse = System.getProperty("SimpleWebserviceClient.debug.response") != null;


	public AbstractInvocationHandler(Class<?> clazz, String endPoint, Validate validate) {
		this.clazz = clazz;
		this.endPoint = endPoint;
		this.validate = validate;
		validator = Validate.NONE.equals(validate) ? null : new ValidationHandler();
	}

	private FaultHandler getFaultHandler() {
		if(lazyFaultHandler == null) {
			lazyFaultHandler  = new FaultHandler(getClassContext());
		}
		return lazyFaultHandler;
	}

	private Class<?>[] getClassContext() {
		if(lazyClazzContext == null) {
			lazyClazzContext = getClassContext(clazz);
		}
		return lazyClazzContext;
	}
	
	protected JAXBContext getContext() throws JAXBException {
		if (lazyContext == null) {
			lazyContext = JAXBContext.newInstance(getClassContext());
		}
		return lazyContext;
	}
	
	private Class<?>[] getClassContext(Class<?> clazz) {
		try {
			XmlSeeAlso seeAlso = Class.forName(clazz.getName()).getAnnotation(XmlSeeAlso.class);
			@SuppressWarnings("rawtypes")
			List<Class> classes = new ArrayList<>(asList(seeAlso.value()));
			classes.add(ObjectFactory.class);
			return reflectionHelper.getAllReturnTypes(classes);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException(e);
		}
	}


	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Object mapped = args[0];
		String requestXml = marshal(mapped);
		if(debugRequest) {
			System.out.println(requestXml);
		}
		HTTP.Response postRequest = postRequest(requestXml);
		if(debugResponse) {
			System.out.println(postRequest.getBody());
		}
		if (postRequest.getStatus() >= 500) {
			getFaultHandler().handleFault(postRequest.getBody());
		}
		if (postRequest.getStatus() >= 400) {
			throw new IOException(format("%s %s - %s", postRequest.getStatus(), postRequest.getStatusText(), endPoint));
		}
		if (postRequest.getStatus() >= 300) {
			System.err.println(format("%s %s - %s", postRequest.getStatus(), postRequest.getStatusText(), endPoint));
		}
		return unMarshal(postRequest.getBody());
	}

	protected abstract HTTP.Response postRequest(String requestXml) throws IOException;

	public <T> T map(Class<T> clazz, Object[] args) throws Exception {
		T instance = clazz.newInstance();
		Iterator<String> props = asList(((XmlType) clazz.getAnnotation(XmlType.class)).propOrder()).iterator();
		for (Object arg : args) {
			reflectionHelper.setProperty(instance, props.next(), arg);
		}
		return instance;
	}

	protected String marshal(Object value) throws JAXBException, PropertyException {
		if(validate.request) validator.validate(value);
		Marshaller jaxbMarshaller = getContext().createMarshaller();
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		jaxbMarshaller.marshal(wrap(value), baos);
		return baos.toString();
	}

	private JAXBElement<?> wrap(Object... any) {
		ObjectFactory objectFactory = new ObjectFactory();
		Body body = new Body();
		for (Object o : any) {
			body.getAny().add(o);
		}
		Envelope envelope = new Envelope();
		envelope.getAny().add(objectFactory.createBody(body));
		return objectFactory.createEnvelope(envelope);
	}
	
	@SuppressWarnings("unchecked")
	protected Object unMarshal(String input) throws JAXBException, PropertyException, UnsupportedEncodingException, MalformedURLException, SAXException {
		Unmarshaller jaxbMarshaller = getContext().createUnmarshaller();
		JAXBElement<Envelope> jb = (JAXBElement<Envelope>) jaxbMarshaller.unmarshal(new ByteArrayInputStream(input.getBytes("UTF-8")));
		Object ret = jb.getValue().getBody().getAny().iterator().next();
		if(validate.response) validator.validate(ret);
		return ret;
	}

}
