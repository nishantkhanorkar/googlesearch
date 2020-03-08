package com.google.search.util;

import org.apache.commons.lang3.StringUtils;

public enum Browsers {

		CHROME("chrome"),
		FIREFOX("firefox"),
		INTERNET_EXPLORER("InternetExplorer");
		
		String name;
		
		/**
		 * @param name
		 */
		Browsers(String name){
			this.name= name;
		}
		
		/**
		 * @param browserName
		 * @return
		 */
		public  static Browsers getBrowserByText(String browserName) {
			
			for( Browsers browser: Browsers.values()) {
				if(StringUtils.equalsIgnoreCase(browser.name, browserName)) {
					return browser;         
				}
			}
			// no match found return CHROME;
			return Browsers.CHROME;
		}  		
}
