package com.google.search;

import com.google.search.base.TestBase;

public class EnvironmentInitializer extends TestBase {

	/**
	 * This will initialize driver without having to interact with it.
	 * 
	 */
	public void initializeTestEnvironment() {
		initialization(); 
	}
	
	/**
	 * This will close the environment an clean up the drivers
	 */
	public void releaseTestEnvironment() {
		release();
	}
}
