package com.baloise.orchestra;

import java.io.File;

import org.junit.Test;

public class PSCHelperTest {

	@Test(expected = IllegalArgumentException.class)
	public void testCreatePscFile() throws Exception {
		PSCHelper helper = new PSCHelper();
		helper.createPscFile(new File("."), new File("bla.psc"), "test", null);
	}

}
