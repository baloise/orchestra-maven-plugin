package com.baloise.orchestra;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Logger;

public interface Log extends Consumer<Object>{
	default void accept(Object o) {
		info(o);
	}
	
	public static Log DEFAULT = new Log() {
		public Logger logger = Logger.getLogger("com.baloise.orchestra");
		
		@Override
		public void warn(Object o) {
			logger.warning(Objects.toString(o));
		}
		
		@Override
		public void info(Object o) {
			logger.info(Objects.toString(o));
		}
		
		@Override
		public void error(Object o) {
			logger.severe(Objects.toString(o));
		}
		
		@Override
		public void debug(Object o) {
			logger.fine(Objects.toString(o));
		}
	};
	void warn(Object o);
	void error(Object o);
	void info(Object o);
	void debug(Object o);
}
