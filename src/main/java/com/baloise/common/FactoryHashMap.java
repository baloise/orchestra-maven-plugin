package com.baloise.common;

import java.util.HashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * How many times have you written code like this
 * <blockquote><pre>
 *  List&lt;Integer&gt; value = map.get(key);
 *  if(value == null) {
 *    value = new ArrayList&lt;&gt;();
 *    // do something to value ...
 *    map.put(key, value);
 *  }
 * </pre></blockquote>
 * 
 * I always felt there should be a map in the JDK that knew how to create an initialize non existing values.
 * <br/>Here it is:
 * <blockquote><pre>
 *  FactoryHashMap&lt;String, List&lt;Integer&gt;&gt; map = FactoryHashMap.create(ArrayList.class);
 * </pre></blockquote>
 * which is equivalent to:
 * <blockquote><pre>
 *  FactoryHashMap&lt;String, List&lt;Integer&gt;&gt; map = FactoryHashMap.create(k -&gt; new ArrayList&lt;&gt;());
 * </pre></blockquote>
 * An example using the key to initialize the value:
 * <blockquote><pre>
 *  Map<String, JButton> buttons = FactoryHashMap.create(text -> new JButton(text));
 * </pre></blockquote>
 * 
 * Of course you can use anonymous classes instead of the create() methods if you prefer. I don't. 
 */
@SuppressWarnings("serial")
public abstract class FactoryHashMap<K, V> extends HashMap<K, V> implements Function<K, V> {

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.HashMap#get(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public V get(Object key) {
		V ret = super.get(key);
		if (ret == null) {
			ret = apply((K) key);
			put((K) key, ret);
		}
		return ret;
	}

	
	public static <K, V> FactoryHashMap<K, V> create(Function<K, V> function) {
		return new FactoryHashMap<K, V>() {
			
			@Override
			public V apply(K t) {
				return function.apply(t);
			}
		};
	}
	
	public static <K, V> FactoryHashMap<K, V> create(Supplier<V> supplier) {
		return create(ignore -> supplier.get());
	}
	
	public static <K, V> FactoryHashMap<K, V> create(V defaultValue) {
		return create(ignore -> defaultValue);
	}

	public static <K, V> FactoryHashMap<K, V> create(Class<? extends V> clazz) {
		return create(key -> instanciate(clazz));
	}

	private static <V> V instanciate(Class<? extends V> clazz) {
		try {
			return clazz.newInstance();
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}
}