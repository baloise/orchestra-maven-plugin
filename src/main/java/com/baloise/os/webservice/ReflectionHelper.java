package com.baloise.os.webservice;

import static java.lang.String.format;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlType;

public class ReflectionHelper {
	
	Map<String, Field> fildCache = new HashMap<>();
	protected Field getField(Class<?> clazz, String property) throws NoSuchFieldException, SecurityException {
		final String key = clazz.getName() + "." + property;
		Field ret = fildCache.get(key);
		if (ret == null) {
			ret = clazz.getDeclaredField(property);
			ret.setAccessible(true);
			fildCache.put(key, ret);
		}
		return ret;
	}
	
	public void setProperty(Object instance, final String property, final Object value) throws Exception {
		getField(instance.getClass(), property).set(instance, value);
	}

	public Object getProperty(Object instance, final String property) throws Exception {
		return getField(instance.getClass(), property).get(instance);
	}
	
	public Class<?>[] getAllReturnTypes(@SuppressWarnings("rawtypes") Collection<Class> classes) {
		Set<Class<?>> ret = new HashSet<>();
		Set<Class<?>> skipped = new HashSet<>();
		for (Class<?> objectFactoryClass : classes) {
			for (Method m : objectFactoryClass.getDeclaredMethods()) {
				final Class<?> returnType = m.getReturnType();
				if(returnType.getAnnotation(XmlType.class) == null) {
					if(!skipped.contains(returnType)) {
						skipped.add(returnType);
						System.err.println(format("Type %s has no XmlType annotation - skipping", returnType));
					}
				} else {
					ret.add(returnType);
				}
			}
		}
		return ret.toArray(new Class<?>[ret.size()]);
	}

}
