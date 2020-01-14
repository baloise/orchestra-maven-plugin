package com.baloise.maven.orchestra;

import static org.junit.Assert.assertTrue;
import static com.baloise.maven.orchestra.DeployHelper.isSuccess;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class DeployHelperTest {

	@Test
	public void testIsSuccess() throws Exception {
		assertTrue(isSuccess("Redeploy finished.", "Redeploy"));
		assertTrue(isSuccess("Redeploy finished without errors", "Redeploy"));
		assertFalse(isSuccess("Redeploy finished without errors", "Deploy"));
		assertFalse(isSuccess("sdfkjsdkfjh", "Redeploy"));
	}

}
