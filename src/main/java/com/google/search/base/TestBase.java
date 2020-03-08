package com.google.search.base;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.events.EventFiringWebDriver;

import com.google.search.listener.WebEventListener;
import com.google.search.util.Browsers;
import com.google.search.util.Constants;
import com.google.search.util.Utilities;

import io.github.bonigarcia.wdm.WebDriverManager;

public class TestBase {

	public static WebDriver driver;

	/**
	 * 
	 */
	public static void initialization() {

		switch (Browsers.getBrowserByText(Utilities.getPropertyValueFromConfig("browser"))) {

		case FIREFOX:
			WebDriverManager.firefoxdriver().setup();
			driver = new FirefoxDriver();
			break;

		case INTERNET_EXPLORER:
			WebDriverManager.iedriver().setup();
			driver = new InternetExplorerDriver();
			break;

		// Setting Chrome as default if any other option is selected.
		case CHROME:
		default:
			WebDriverManager.chromedriver().setup();
			ChromeOptions option = new ChromeOptions();
			File extension = new File(Constants.CHROME_EXTENSION_INPUTTOOLS);
			option.addExtensions(extension);
			DesiredCapabilities chrome = DesiredCapabilities.chrome();
			chrome.setJavascriptEnabled(true);
			option.setCapability(ChromeOptions.CAPABILITY, option);
			driver = new ChromeDriver(option);
			break;
		}
		EventFiringWebDriver e_driver = new EventFiringWebDriver(driver);
		// Now create object of EventListerHandler to register it with
		// EventFiringWebDriver
		WebEventListener eventListener = new WebEventListener();
		driver = e_driver.register(eventListener);

		driver.manage().window().maximize();
		driver.manage().deleteAllCookies();
		driver.manage().timeouts().pageLoadTimeout(Constants.PAGE_LOAD_TIMEOUT, TimeUnit.SECONDS);
		driver.manage().timeouts().implicitlyWait(Constants.IMPLICIT_WAIT, TimeUnit.SECONDS);

	}

	/**
	 * 
	 */
	public static void release() {
		driver.quit();
		driver = null;
	}
}
