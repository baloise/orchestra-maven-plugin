package com.baloise.os.webservice;

public enum Validate {
	NONE(false, false),
	REQUEST(true, false),
	RESPONSE(false, true),
	BOTH(true, true);

	public final boolean request;
	public final boolean response;
	private Validate(boolean request, boolean response) {
		this.request = request;
		this.response = response;
	}
	
}
