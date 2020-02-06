package com.rsi.selenium;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RsiChromeTester {
	final static Logger logger = Logger.getLogger(RsiChromeTester.class);
	private WebDriver driver;
	private ChromeOptions options = new ChromeOptions();
	public WebDriver getDriver() {
		return driver;
	}

	public RsiChromeTester() {
		System.setProperty("webdriver.chrome.driver", "C:\\java_libs\\chromedriver.exe");
		options.addArguments("--headless");

		driver = new ChromeDriver();

		
	}
	public String loginToApp(String url_to_test, String userNameField, String passwordField, String btnField, 
			String userName, String password, String successElement) throws NoSuchElementException {
		String status = "Failed";
		logger.debug("PARAMS ARE:" +url_to_test+userNameField+passwordField+btnField+userName+password);
		driver.get(url_to_test);
        // Alternatively the same thing can be done like this
        
		// TODO Prestep - Prior to login the code needs to browse to the verify step.
		//if (url_to_test.contains("dev11.resourcestack.com")) {
		if(commonHealthCoreUrl(url_to_test)) {
			WebElement emailElement = driver.findElement(By.xpath("/html/body/div[1]/div/div/div/div/div/div/div[4]/form/fieldset/div[1]/div/input"));
			emailElement.sendKeys(userName);
			WebElement verifyElement = driver.findElement(By.xpath("/html/body/div[1]/div/div/div/div/div/div/div[4]/form/fieldset/div[2]/div/button"));
			verifyElement.click();
			try {
				TimeUnit.SECONDS.sleep(15);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

        WebElement userNameElement = driver.findElement(By.id(userNameField));
		WebElement passwordElement = driver.findElement(By.id(passwordField));

		userNameElement.sendKeys(userName);
		passwordElement.sendKeys(password);
		
		// find submit button and click
		try {
			driver.findElement(By.name(btnField)).click();

		} catch (NoSuchElementException nse) {
			logger.error("btnField [" + btnField + "] with id not found. try and see if hitting Enter works on a submit form");
			passwordElement.sendKeys(Keys.ENTER);

		}

		if(commonHealthCoreUrl(url_to_test)) {
			WebDriverWait wait = new WebDriverWait(driver, 3000);
			wait.until(ExpectedConditions.alertIsPresent());
			Alert alert = driver.switchTo().alert();
			alert.accept();

			wait.until(ExpectedConditions.alertIsPresent());
			alert = driver.switchTo().alert();
			alert.accept();
		}


		// on the result page get page title and some element
		String titleOfResultPage = driver.getTitle();
		logger.debug("Page title is: " + driver.getTitle());
		if(titleOfResultPage.equals("Successfully logged in")) {
			// Check the title of the page
			status = "Success";
			logger.debug("Page title is: " + driver.getTitle());
        }
        //driver.quit();
		
		return status;
	}

	private boolean commonHealthCoreUrl(String url_to_test) {
		String[] chcUrls = {"dev11.resourcestack.com", "demo.commonhealthcore.org"};
		boolean status = false;

		for(String s : chcUrls) {
			if(url_to_test.contains(s)) {
				status = true;
			}
		}

		return status;
	}

	public String testPageElement(Connection conn, String url, String loginName, String loginPwd, String fieldName, String xpath, String fieldType, String readElement, String need_screenshot, String initialDescription, int currentSchedulerId, int currentTestCaseId, int currentTestSequence) {
		String status = "Initial";
		WebElement userNameElement = null;
		String startTime = com.rsi.utils.RsiTestingHelper.returmTimeStamp();
		String endTime = null;
		String description = initialDescription;
		String valueOfElement = null;

		try {
			if ((fieldName == null || fieldName.trim().length() == 0) || (xpath != null && xpath.trim().length() > 0)) {
				if(xpath == null || xpath.trim().length() == 0){
					//TODO now check to see if we can identify the element from readElement. (Sameer 02012020).
					if(!com.rsi.utils.RsiTestingHelper.checkEmpty(readElement)) {
						// Now try to identify the first occurence of fieldType for the readElement.
						if(fieldType.equalsIgnoreCase("h3")) {
							List<WebElement> elements = driver.findElements(By.tagName("h3"));

							// now iterate through the list of elements and see if anyone has the content you are looking for.
							for (WebElement w : elements) {
								if (w.getText().equalsIgnoreCase(readElement) || w.getText().matches(readElement.substring(1, readElement.length() - 1))) {
									status = "Success";
								}
							}
						}
						else if(fieldType.equalsIgnoreCase("b")) {
							List<WebElement> elements = driver.findElements(By.tagName("b"));

							// now iterate through the list of elements and see if anyone has the content you are looking for.
							for (WebElement w : elements) {
								if (w.getText().equalsIgnoreCase(readElement) || w.getText().matches(readElement.substring(1, readElement.length() - 1))) {
									status = "Success";
								}
							}
						}

					}
					else {
						description = description.concat(" read element was blank. dont know how to compare.");
						status = "Failed";
					}

				}
				else {
					userNameElement = driver.findElement(By.xpath(xpath));
				}
			}
			else {
				userNameElement = driver.findElement(By.id(fieldName));
			}

			if(fieldType.equalsIgnoreCase("label") || fieldType.equalsIgnoreCase("text")) {
				valueOfElement = userNameElement.getAttribute("value");
			} else if(fieldType.equalsIgnoreCase("th") || fieldType.equalsIgnoreCase("td")) {
				valueOfElement = userNameElement.getText();
			}

			logger.debug("Page title is: " + driver.getTitle());
			if(status.equalsIgnoreCase("Initial")) {
				if(!com.rsi.utils.RsiTestingHelper.checkEmpty(valueOfElement) || !com.rsi.utils.RsiTestingHelper.checkEmpty(readElement)) {
					if(readElement.startsWith("{") && readElement.endsWith("}")){
						// We know that we need to check a regular expression
						if(valueOfElement.matches(readElement.substring(1,readElement.length()-1))) {
							status = "Success";
						}
						else {
							status = "Failed";
							description = description.concat(" - No read element or value onthe page to inspect");
						}
					}
					else {
						if(valueOfElement.equalsIgnoreCase(readElement)) {
							status = "Success";
						}
						else {
							status = "Failed";
							description = description.concat(" - No read element or value onthe page to inspect");
						}
					}

				}
				else {
					status = "Success";
				}
				logger.info("Element found " + fieldName + " successfully.");
			}
			endTime = com.rsi.utils.RsiTestingHelper.returmTimeStamp();
		} catch (NoSuchElementException nse) {
			nse.printStackTrace();
			logger.error("Error no element found on the page " + nse.getMessage());
			description = description.concat(" - " + nse.getMessage());
			status = "Failure";
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error other exception " + e.getMessage());
			description = description.concat(" - " + e.getMessage());
			status = "Failure";
		}
		endTime = com.rsi.utils.RsiTestingHelper.returmTimeStamp();
		if(!status.equalsIgnoreCase("SUCCESS")){
			long resultCaseId = updateTestCaseWithError(conn, currentTestCaseId, currentSchedulerId, startTime, endTime, description);
			takeScreenshot(conn, resultCaseId);
		}
		else {
			long resultCaseId = updateTestCaseWithSuccess(conn, currentTestCaseId, currentSchedulerId, startTime, endTime, description);
			if(!com.rsi.utils.RsiTestingHelper.checkEmpty(need_screenshot)) {
				takeScreenshot(conn, resultCaseId);
			}
		}
		
		return status;
	}
	//TODO Add Action URL param. to this method. That way upon successful completion of the action, we will be able to check if the action performed successfully.
	public String actionPageElement(Connection conn, String url, String loginName, String loginPwd, String fieldName, String fieldType, String readElement, String xpath, String action, String actionUrl, String baseURL, String need_screenshot, String initialDescription, int currentSchedulerId, int currentTestCaseId, int currentTestSequence) {
		String status = "Initial";
		WebElement clickableElement = null;
		String startTime = com.rsi.utils.RsiTestingHelper.returmTimeStamp();
		String endTime = "";
 		String description = initialDescription;

		try {
			String currentPageUrl = driver.getCurrentUrl();
			if (!currentPageUrl.equalsIgnoreCase(baseURL) && currentTestSequence == 1)
				driver.get(baseURL);

			if (fieldType.equalsIgnoreCase("anchor")) {
				if(!com.rsi.utils.RsiTestingHelper.checkEmpty(fieldName)){
					clickableElement = driver.findElement(By.linkText(fieldName));
					clickableElement.sendKeys(Keys.ENTER);
				}
				else if (!com.rsi.utils.RsiTestingHelper.checkEmpty(xpath)) {
					if(action.equalsIgnoreCase("click")) {
						clickableElement = driver.findElement(By.xpath(xpath));
						clickableElement.click();
					}
					else{
						clickableElement = driver.findElement(By.xpath(xpath));
						clickableElement.sendKeys(Keys.ENTER);
					}

				}

				// TODO new method checkStatus will decide whether or not action resulted in success or failure. params should be actionUrl, readElement. one of the to should be populated.
				status = checkStatus(url, readElement, actionUrl);
			}
			else if(fieldType.equalsIgnoreCase("span") || fieldType.equalsIgnoreCase("text") || fieldType.equalsIgnoreCase("radio") || fieldType.equalsIgnoreCase("checkbox") || fieldType.equalsIgnoreCase("button") || fieldType.equalsIgnoreCase("div") || fieldType.equalsIgnoreCase("b") || fieldType.equalsIgnoreCase("h3")) {
				if(com.rsi.utils.RsiTestingHelper.checkEmpty(fieldName) || !com.rsi.utils.RsiTestingHelper.checkEmpty(xpath)){
					if(com.rsi.utils.RsiTestingHelper.checkEmpty(xpath)) {
						status = "Failed";
						description = description.concat(" - " + " No element id found on the test case.");
					}
					else {
						clickableElement = driver.findElement(By.xpath(xpath));
					}
				}
				else {
					try {
						clickableElement = driver.findElement(By.id(fieldName));
					} catch (NoSuchElementException nse) {
						// we will try one more time by checking all elements of the field type with getText() of fieldName.
						List<WebElement> elements = driver.findElements(By.tagName(fieldType));
						for (WebElement e : elements) {
							if(e.getText().equalsIgnoreCase(fieldName)) {
								clickableElement = e;
							}
						}
					}
				}

			}
			//NEXT Section should only be called if the status has not been populated as yet. Which in the case of anchor tag is already taken care of. (Sameer 01262020)
			if(status.equalsIgnoreCase("initial") && clickableElement != null){
				if(action.equalsIgnoreCase("Click") ) {
					clickableElement.click();
					if(!com.rsi.utils.RsiTestingHelper.checkEmpty(actionUrl))
						status = checkStatus(url, readElement, actionUrl);
					else
						status = "Success";
				}
				else if(action.equalsIgnoreCase("tab")) {
					clickableElement.sendKeys(Keys.TAB);
					if(!com.rsi.utils.RsiTestingHelper.checkEmpty(actionUrl))
						status = checkStatus(url, readElement, actionUrl);
					else
						status = "Success";
				}
			} else {
				if (status.equalsIgnoreCase("initial"))
					status = "failed";
			}

		}catch (NoSuchElementException nse) {
			nse.printStackTrace();
			logger.error("Error no element found on the page " + nse.getMessage());
			description = description.concat(" - " + nse.getMessage() + ".");
			status = "Failure";
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error other exception " + e.getMessage());
			description = description.concat(" - " + e.getMessage() + ".");
			status = "Failure";
		}
		endTime = com.rsi.utils.RsiTestingHelper.returmTimeStamp();
		if(!status.equalsIgnoreCase("SUCCESS")){
			long resultCaseId = updateTestCaseWithError(conn, currentTestCaseId, currentSchedulerId, startTime, endTime, description);
			takeScreenshot(conn, resultCaseId);
		}
		else {
			long resultCaseId = updateTestCaseWithSuccess(conn, currentTestCaseId, currentSchedulerId, startTime, endTime, description);
			if(!com.rsi.utils.RsiTestingHelper.checkEmpty(need_screenshot)) {
				takeScreenshot(conn, resultCaseId);
			}
		}
		
		return status;
	}

	private String checkStatus(String url, String readElement, String actionUrl) {
		// TODO if url is where the page is at or if readELement is true then return success.
		if(!com.rsi.utils.RsiTestingHelper.checkEmpty(actionUrl)){
			if (actionUrl.equals(driver.getCurrentUrl())){
				return "Success";
			}
			else {
				logger.debug("In check status method returning false since actionUrl [" + actionUrl + "] is not the same as the url [" + driver.getCurrentUrl() + "]");
				return "Failed";
			}
		}

		return "Success";
	}

	public String inputPageElement(Connection conn, String url, String loginName, String loginPwd, String fieldName, String field_type, String inputValue, String xpath, String base_url, String need_screenshot,  String initialDescription, int currentSchedulerId, int currentTestCaseId, int currentTestSequence) {
		String status = "Failed";
		WebElement element = null;
		String startTime = com.rsi.utils.RsiTestingHelper.returmTimeStamp();
		String endTime = "";
		String currentPageUrl = driver.getCurrentUrl();
		String description = initialDescription;
		try {
			if (!currentPageUrl.equalsIgnoreCase(base_url) && currentTestSequence == 1)
				driver.get(base_url);

			if((fieldName == null || fieldName.trim().length() == 0) || (xpath != null && xpath.trim().length() > 0)) {
				element = driver.findElement(By.xpath(xpath));
			}
			else {
				element = driver.findElement(By.id(fieldName));
			}

			element.sendKeys(inputValue);
			// TODO new method checkStatus will decide whether or not action resulted in success or failure. params should be actionUrl, readElement. one of the to should be populated.
			status = checkStatus(url, fieldName, "");
		}catch (NoSuchElementException nse) {
			nse.printStackTrace();
			logger.error("Error no element found on the page " + nse.getMessage());
			description = description.concat(" - " + nse.getMessage() + ".");
			status = "Failure";
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error other exception " + e.getMessage());
			description = description.concat(" - " + e.getMessage() + ".");
			status = "Failure";
		}
		endTime = com.rsi.utils.RsiTestingHelper.returmTimeStamp();
		if(!status.equalsIgnoreCase("SUCCESS")){
			long resultCaseId = updateTestCaseWithError(conn, currentTestCaseId, currentSchedulerId, startTime, endTime, description);
			takeScreenshot(conn, resultCaseId);
		}
		else {
			long resultCaseId = updateTestCaseWithSuccess(conn, currentTestCaseId, currentSchedulerId, startTime, endTime, description);
			if(!com.rsi.utils.RsiTestingHelper.checkEmpty(need_screenshot)) {
				takeScreenshot(conn, resultCaseId);
			}
		}
		return status;
	}

	private long updateTestCaseWithSuccess(Connection conn, int currentTestCaseId, int currentSchedulerId, String startTime, String endTime, String description) {
		long newResultCaseId = -1;
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO result_cases (rd_id,test_case_id,scheduler_id, error_description, created_at, updated_at) VALUES(?,?,?,?,STR_TO_DATE(?,'%Y-%m-%d %H:%i:%s'),STR_TO_DATE(?,'%Y-%m-%d %H:%i:%s'))");
			pstmt.setInt(1, 1);
			pstmt.setInt(2, currentTestCaseId);
			pstmt.setInt(3, currentSchedulerId);
            pstmt.setString(4,description);
			pstmt.setString(5,startTime);
			pstmt.setString(6, endTime);
			if (pstmt.execute()) {
				ResultSet rs = pstmt.getGeneratedKeys();

				if (rs.next()) {
					newResultCaseId = rs.getLong(1);
					logger.info("Inserted ID -" + newResultCaseId); // display inserted record
				}
				logger.info("Inserted TestCase Result id [" + currentTestCaseId + " ], with Success");
			}
			else {
				logger.error("Could not inserted the Test Case id in Results [" + currentTestCaseId + " ] with Error Status. Please delete it manually. ");
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.error("Could not update the Test Case with Result for id [" + currentTestCaseId + " ] with Success Status. Please delete it manually. ");
			e.printStackTrace();
		} finally {
			try {
				pstmt.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return newResultCaseId;

	}

	private String takeScreenshot(Connection conn, long resultCaseId) {
		String fileName = new Long(resultCaseId).toString()  +".png";
		try
		{ TakesScreenshot ts=(TakesScreenshot)driver;

			File source=ts. getScreenshotAs(OutputType. FILE);
			FileUtils. copyFile(source, new File("./Screenshots/"+ fileName));
			logger.info("Screenshot taken");
			// Now save the file name to the database.
			PreparedStatement pstmt = conn.prepareStatement("UPDATE result_cases SET screenshot_file_location = ? WHERE id = ?");
			pstmt.setString(1,fileName);
			pstmt.setLong(2, resultCaseId);

			pstmt.execute();

		} catch (Exception e) {
			logger.error("Exception while taking screenshot " + e.getMessage());
		}
		return fileName;
	}

	private long updateTestCaseWithError(Connection conn, int currentTestCaseId, int currentSchedulerId, String startTime, String endTime, String description) {
		long newResultCaseId = -1;
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO result_cases (rd_id,test_case_id,scheduler_id, error_description, created_at, updated_at) VALUES(?,?,?,?, STR_TO_DATE(?,'%Y-%m-%d %H:%i:%s'),STR_TO_DATE(?,'%Y-%m-%d %H:%i:%s'))", Statement.RETURN_GENERATED_KEYS);
			pstmt.setInt(1, 2);
			pstmt.setInt(2, currentTestCaseId);
			pstmt.setInt(3, currentSchedulerId);
			pstmt.setString(4, description);
			pstmt.setString(5, startTime);
			pstmt.setString(6, endTime);
			pstmt.execute();
			ResultSet rs = pstmt.getGeneratedKeys();

			if (rs.next()) {
				newResultCaseId = rs.getLong(1);
				logger.info("Inserted ID -" + newResultCaseId); // display inserted record
			}
			logger.info("Inserted TestCase Result id [" + currentTestCaseId + " ], with Error");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.error("Could not update the Test Case with Result for id [" + currentTestCaseId + " ] with Error Status. Please delete it manually. ");
			e.printStackTrace();
		} finally {
			try {
				pstmt.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return newResultCaseId;
	}

	public boolean switchToNewTab() {
		ArrayList<String> tabs = new ArrayList<String> (driver.getWindowHandles());
		logger.debug("number of tabs are " + tabs.size());
		driver.switchTo().window(tabs.get(1));

		return true;
	}
}
