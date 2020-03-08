package com.google.search.pages;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.testng.Assert;

import com.google.search.base.TestBase;

public class SearchResults extends TestBase {

	@FindBy(tagName = "body")
	WebElement body;

	/**
	 * 
	 */
	public SearchResults() {
		PageFactory.initElements(driver, this);
	}

	/**
	 * @param searchText
	 */
	public void verifySearchResults(String searchText) {

		Assert.assertTrue(StringUtils.contains(driver.getTitle(), searchText));
		Assert.assertTrue(StringUtils.contains(body.getText(), searchText));

	}
}
