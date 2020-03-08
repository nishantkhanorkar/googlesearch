package com.google.search.testcases;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.search.EnvironmentInitializer;
import com.google.search.pages.GoogleHomePage;
import com.google.search.pages.GoogleInputTools;
import com.google.search.pages.SearchResults;

public class GoogleSearch {

	private EnvironmentInitializer init;

	/**
	 * 
	 */
	@BeforeMethod
	public void setUp() {
		init = new EnvironmentInitializer();
		init.initializeTestEnvironment();
	}

	/**
	 * for now using hardcoded value but this can be parameterized at a 
	 * later stage
	 * 
	 * 
	 */
	@Test(priority = 1)
	public void searchByTypingKeyword() {

		String searchText = "Hello World";
		GoogleHomePage g = new GoogleHomePage();
		g.navigateToGoogleHomePage();
		g.enterTextToSearch(searchText);

		SearchResults s = new SearchResults();
		s.verifySearchResults(searchText);

	}

	/**
	 * Using a hardcoded value to search using the virtual keyboard.
	 * 
	 */
	@Test(priority = 2)
	public void searchByUsingVirtualKeyboard() {
		String searchText = "Hello World";

		GoogleHomePage hp = new GoogleHomePage();
		hp.navigateToGoogleHomePage();
		
		GoogleInputTools git = new GoogleInputTools();
		git.displayVirtualKeyboard();
		
		hp.inputValueInSearchBarUsingVirtualKeyboard(searchText);
		
		SearchResults sr = new SearchResults();
		sr.verifySearchResults(searchText);
	}

	/**
	 * 
	 */
	@AfterMethod
	public void tearDown() {
		init.releaseTestEnvironment();
	}
}
