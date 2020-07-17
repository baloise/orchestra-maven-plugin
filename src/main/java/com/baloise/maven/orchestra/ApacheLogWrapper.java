package com.baloise.maven.orchestra;

import java.util.Objects;

import com.baloise.orchestra.Log;

public class ApacheLogWrapper implements Log {
	
	final org.apache.maven.plugin.logging.Log delegate;

	
	public ApacheLogWrapper(org.apache.maven.plugin.logging.Log delegate) {
		this.delegate = delegate;
	}

	@Override
	public void warn(Object o) {
		delegate.warn(Objects.toString(o));
	}

	@Override
	public void error(Object o) {
		delegate.error(Objects.toString(o));
	}

	@Override
	public void info(Object o) {
		delegate.info(Objects.toString(o));
	}

	@Override
	public void debug(Object o) {
		delegate.debug(Objects.toString(o));
	}

}
