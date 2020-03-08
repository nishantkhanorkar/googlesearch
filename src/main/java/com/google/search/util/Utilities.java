package com.google.search.util;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.Assert;

import com.google.search.base.TestBase;

public class Utilities extends TestBase {

	/**
	 * private constructor to stop initialization
	 */
	private Utilities() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Get property value from the configuration file
	 * 
	 * @param propertyKey
	 * @return
	 */
	public static String getPropertyValueFromConfig(String propertyKey) {
		try {
			Properties prop = new Properties();
			prop.load(Utilities.class.getResourceAsStream(Constants.CONFIG_PATH));
			// return a blank value in order to avoid NPEs
			return prop.getProperty(propertyKey, "");
		} catch (IOException e) {
			Assert.fail("Exception occured while reading config file " + e.getMessage());
			// test will never reach this line as the previous should halt the
			// execution as there was an error getting the property value
			return "";
		}
	}
	
	/**
	 * @param inputChar
	 * @return
	 */
	public static String getXpathForKeyboadCharacter(char  inputChar) {
		
		// Tested for digits, characters  and spaces	
		char key = (Character.isLowerCase(inputChar))? Character.toUpperCase(inputChar) : inputChar;
		return new StringBuilder().append("//*[@id='K").append((int)key).append("']").toString();	
	}
	
	/**
	 * 
	 */
	public static void pause() {	
		try {
			Thread.sleep(1000);
		} catch (Exception e) {
			Assert.fail("Exception occured while wating " + e.getMessage());
		}
	}
	
	/**
	 * 
	 */
	public static void takeScreenshot() {
		File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
		String currentDir = System.getProperty("user.dir");
		try {
			FileUtils.copyFile(scrFile, new File(currentDir + "/screenshots/" + System.currentTimeMillis() + ".png"));
		} catch (IOException e) {
			//Intentionally failing this as screenshots are important
			Assert.fail("Exception occured while taking screenshots file " + e.getMessage());
		}
	}
}
