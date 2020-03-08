package com.google.search.pages;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.testng.Assert;

import com.google.search.base.TestBase;
import com.google.search.util.Utilities;

public class GoogleHomePage extends TestBase {

	// pageObjects
	@FindBy(xpath = "//*[@name='q']")
	WebElement searchBox;

	@FindBy(xpath = "//*[@id='kbd']")
	WebElement virtualKeyboard;

	@FindBy(id = "K16")
	WebElement vkbd_Shift;

	/**
	 * 
	 */
	public GoogleHomePage() {
		PageFactory.initElements(driver, this);
	}

	/**
	 * @param searchText
	 */
	public void enterTextToSearch(String searchText) {
		searchBox.sendKeys(searchText);
		searchBox.sendKeys(Keys.ENTER);
	}

	/**
	 * @param searchText
	 */
	public void inputValueInSearchBarUsingVirtualKeyboard(String searchText) {

		// keyboard is displayed in second iframe
		if (StringUtils.isNotBlank(searchText)) {
			Utilities.pause();
			driver.switchTo().frame(1);
			Utilities.pause();
			Assert.assertTrue(virtualKeyboard.isDisplayed(), "Virtual keyboard is not displayed.");
			for (char searchTextChar : searchText.toCharArray()) {
				if (Character.isUpperCase(searchTextChar))
					vkbd_Shift.click();
				driver.findElement(By.xpath(Utilities.getXpathForKeyboadCharacter(searchTextChar))).click();
			}
		}
		driver.switchTo().defaultContent();
		searchBox.sendKeys(Keys.ENTER);
	}

	/**
	 * @param searchText
	 */
	public void verifySearchBoxText(String searchText) {

		// after clicking all the elements verify the text is entered properly.
		Assert.assertTrue(StringUtils.equals(searchText, searchBox.getText()));
	}

	/**
	 * 
	 */
	public void navigateToGoogleHomePage() {
		driver.get(Utilities.getPropertyValueFromConfig("AutUrl"));
	}
}
