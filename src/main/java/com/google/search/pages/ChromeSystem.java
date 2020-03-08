package com.google.search.pages;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.testng.Assert;

import com.google.search.base.TestBase;
import com.google.search.util.Constants;
import com.google.search.util.Utilities;

public class ChromeSystem extends TestBase {

	@FindBy(id = "expandAll")
	WebElement btn_ExpandAll;

	@FindBy(id = "extensions-value")
	WebElement div_extensionValues;

	/**
	 * 
	 */
	public ChromeSystem() {
		PageFactory.initElements(driver, this);
	}

	/**
	 * 
	 */
	public void navigateToChromeSystem() {

		// Use robot class to press Ctrl+t keys
		Robot robot;
		try {
			robot = new Robot();

			robot.keyPress(KeyEvent.VK_CONTROL);
			robot.keyPress(KeyEvent.VK_T);
			robot.keyRelease(KeyEvent.VK_CONTROL);
			robot.keyRelease(KeyEvent.VK_T);
		} catch (AWTException e) {
			Assert.fail("Exception Occured while opening a new tab on browser " + e.getMessage());
		}
		// Switch focus to new tab
		ArrayList<String> tabs = new ArrayList<>(driver.getWindowHandles());
		driver.switchTo().window(tabs.get(1));
		driver.get(Utilities.getPropertyValueFromConfig("chromeSystemUrl"));
	}

	/**
	 * @return
	 * @throws InterruptedException
	 */
	public String getIdForInputToolsExtension() {

		btn_ExpandAll.click();
		String extensionValues = div_extensionValues.getText();
		String id = null;
		for (String extension : extensionValues.replaceAll("\n", ",").split(",")) {
			if (StringUtils.contains(extension, Constants.GOOGLE_INPUT_TOOLS))
				id = extension.split(":")[0].trim();
		}
		Assert.assertNotNull(id, "Extension Id not found");
		return id;
	}
}
