package com.google.search.util;

public class Constants {

	/**
	 * private constructor to stop initialization
	 */
	private Constants() {
		throw new UnsupportedOperationException();
	}

	public static final String CONFIG_PATH = "/config/config.properties";
	public static final String CHROME_EXTENSION_INPUTTOOLS = "src/main/resources/chromeExtension/GoogleInputTool.crx";

	public static final  long PAGE_LOAD_TIMEOUT = 20;
	public static final long IMPLICIT_WAIT = 20;
	public static final String  GOOGLE_INPUT_TOOLS = "Google Input Tools";

}
