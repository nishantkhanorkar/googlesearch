package com.google.search.pages;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.testng.Assert;

import com.google.search.base.TestBase;
import com.google.search.util.Utilities;

public class GoogleInputTools extends TestBase {

	@FindBy(id = ":1y.label")
	WebElement optlst_addkeyboardEnglish;

	@FindBy(xpath = "//*[@id=':8d.label']/span[3]")
	WebElement optlst_selectedKeyboardEnglish;

	@FindBy(id = "input_chext_arrow_button_right")
	WebElement btn_rightArrow;

	@FindBy(xpath = "//span[text() = 'Show Keyboard'] ")
	WebElement span_ShowKeyboard;

	/**
	 * 
	 */
	public GoogleInputTools() {
		PageFactory.initElements(driver, this);
	}

	/**
	 * @param extensionId
	 */
	public void navigateToInputTools(String extensionId) {
		driver.get(Utilities.getPropertyValueFromConfig("extensionConfigUrl").replace("%extensionId%", extensionId));
		Utilities.pause();
	}

	/**
	 * 
	 */
	public void setEnglishKeyboard() {
		optlst_addkeyboardEnglish.click();
		btn_rightArrow.click();
		Utilities.pause();
		Assert.assertTrue(optlst_selectedKeyboardEnglish.isDisplayed(), "English keyboard is not selected");
		Assert.assertTrue(StringUtils.equalsIgnoreCase(optlst_selectedKeyboardEnglish.getText(), "English"),
				"English keyboard is not selected");
	}

	/**
	 * @param extensionId
	 */
	public void showKeyboard(String extensionId) {
		driver.navigate()
				.to(Utilities.getPropertyValueFromConfig("extensionPopupUrl").replace("%extensionId%", extensionId));		
		span_ShowKeyboard.click();
	}

	/**
	 * 
	 */
	public void displayVirtualKeyboard() {
		// store the initial handle 
		String originalHandle = driver.getWindowHandle();

		ChromeSystem s = new ChromeSystem();
		
		s.navigateToChromeSystem();
		String extensionId =s.getIdForInputToolsExtension();
		
		// set English Keyboard
		navigateToInputTools(extensionId);
		setEnglishKeyboard();
		activateGoogleInputTool();
		showKeyboard(extensionId);
		
		// return to original handle
		driver.switchTo().window(originalHandle);
	}

	/**
	 * 
	 */
	public void activateGoogleInputTool() {
		Robot robot;
		try {
			robot = new Robot();
			// Press ALT + SHIFT +N to activate the the extension
			robot.keyPress(KeyEvent.VK_ALT);
			robot.keyPress(KeyEvent.VK_SHIFT);
			robot.keyPress(KeyEvent.VK_N);
			
			robot.keyRelease(KeyEvent.VK_N);			
			robot.keyRelease(KeyEvent.VK_SHIFT);
			robot.keyRelease(KeyEvent.VK_ALT);
		} catch (AWTException e) {
			Assert.fail( "Exception occured in while activating google input tool");
		}
	}
}
