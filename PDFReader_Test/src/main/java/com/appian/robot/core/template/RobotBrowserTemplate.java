package com.appian.robot.core.template;

import org.apache.commons.lang.StringUtils;

import com.novayre.jidoka.browser.api.EBrowsers;
import com.novayre.jidoka.browser.api.IWebBrowserSupport;
import com.novayre.jidoka.client.api.IJidokaServer;
import com.novayre.jidoka.client.api.IRobot;
import com.novayre.jidoka.client.api.JidokaFactory;
import com.novayre.jidoka.client.api.annotations.Robot;
import com.novayre.jidoka.client.api.multios.IClient;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Browser robot template. 
 */
@Robot
public class RobotBrowserTemplate implements IRobot {

	/**
	 * URL to navigate to.
	 */
	private static final String HOME_URL = "https://www.appian.com";
	
	/** The JidokaServer instance. */
	private IJidokaServer<?> server;
	
	/** The IClient module. */
	private IClient client;
	
	/** WebBrowser module */
	private IWebBrowserSupport browser;

	/** Browser type parameter **/
	private String browserType = null;

	/**
	 * Action "startUp".
	 * <p>
	 * This method is overrriden to initialize the Appian RPA modules instances.
	 */
	@Override
	public boolean startUp() throws Exception {
		
		server = (IJidokaServer< ? >) JidokaFactory.getServer();

		client = IClient.getInstance(this);
		
		browser = IWebBrowserSupport.getInstance(this, client);

		return IRobot.super.startUp();

	}
	
	/**
	 * Action "start".
	 */
	public void start() {
		server.setNumberOfItems(1);
	}


	/**
	 * Open Web Browser
	 * @throws Exception
	 */
	public void openBrowser() throws Exception  {

		browserType = server.getParameters().get("Browser");
		
		// Select browser type
		if (StringUtils.isBlank(browserType)) {
			server.info("Browser parameter not present. Using the default browser CHROME");
			browser.setBrowserType(EBrowsers.CHROME);
			browserType = EBrowsers.CHROME.name();
		} else {
			EBrowsers selectedBrowser = EBrowsers.valueOf(browserType);
			browserType = selectedBrowser.name();
			browser.setBrowserType(selectedBrowser);
			server.info("Browser selected: " + selectedBrowser.name());
		}
		
		// Set timeout to 60 seconds
		browser.setTimeoutSeconds(60);

		// Init the browser module
		browser.initBrowser();

		//This command is uses to make visible in the desktop the page (IExplore issue)
		if (EBrowsers.INTERNET_EXPLORER.name().equals(browserType)) {
			client.clickOnCenter();
			client.pause(3000);
		}

	}
	public void readPDF() throws Exception{
		//Loading an existing document
		File file = new File("D://MK Dossier.pdf");
		PDDocument document = PDDocument.load(file);
		//Instantiate PDFTextStripper class
		PDFTextStripper pdfStripper = new PDFTextStripper();
		//pdfStripper.setWordSeparator(" ");
		pdfStripper.setLineSeparator("//");
		//pdfStripper.setSortByPosition(true);
		boolean beadsFound = pdfStripper.getSeparateByBeads();
		server.info("beadsFound " + beadsFound);
		pdfStripper.setShouldSeparateByBeads(beadsFound);
		//pdfStripper.setShouldSeparateByBeads(true);
		//Retrieving text from PDF document
		String text = pdfStripper.getText(document);
		server.info(text);
		String str[] = text.split("//");
		List<String> al = new ArrayList<String>();
		al = Arrays.asList(str);
		for(String s: al) {
			if (s == null || s == "") {
				server.info(s);
			}

		}
		//Closing the document
		document.close();
	}

	public void readPDFJAcinth() throws Exception{
		//Loading an existing document
		File file = new File("D://MK Dossier.pdf");
		PDDocument document = PDDocument.load(file);
		//Instantiate PDFTextStripper class
		PDFTextStripper pdfStripper = new PDFTextStripper();
		String text = pdfStripper.getText(document);
		//server.info("trim"+text.trim());
		String test =text.replaceAll("\\s","@");
		//test.replace("&","");
		//server.info("after replace"+test);
		String[] list= test.split("@{137}\\bTOTAL\\b");
		for (String a : list) {

			Pattern pattern =Pattern.compile("^@{12,15}[0-9]");
			Matcher matcher = pattern.matcher(a);
			boolean matchFound = matcher.find();
			//server.info(matcher.start());
			if(matchFound) {
				String substring="";
				//server.info(a);
				Pattern abc = Pattern.compile("[0-9]");
				Matcher def = abc.matcher(a);
				while(def.find()) {
					 substring=a.substring(def.start());
					 break;
				}
				//server.info(a);
				int  b = substring.indexOf("@");
				//server.info(b);
				String c= substring.substring(0,b);
				server.info("Values  :"+c);
			}

		}

		/*String[] list=test.split("[0-9][6]TOTAL");
		for (String a : list) {
			server.info("rockstar");
			server.info(a);
		}*/

		document.close();
	}



	/**
	 * Navigate to Web Page
	 * 
	 * @throws Exception
	 */
	public void navigateToWeb() throws Exception  {
		
		server.setCurrentItem(1, HOME_URL);
		
		// Navegate to HOME_URL address
		browser.navigate(HOME_URL);

		// we save the screenshot, it can be viewed in robot execution trace page on the console
		server.sendScreen("Screen after load page: " + HOME_URL);
		
		server.setCurrentItemResultToOK("Success");
	}

	/**
	 * @see com.novayre.jidoka.client.api.IRobot#cleanUp()
	 */
	@Override
	public String[] cleanUp() throws Exception {
		
		browserCleanUp();
		return null;
	}

	/**
	 * Close the browser.
	 */
	private void browserCleanUp() {

		// If the browser was initialized, close it
		if (browser != null) {
			try {
				browser.close();
				browser = null;

			} catch (Exception e) { // NOPMD
			// Ignore exception
			}
		}

		try {
			
			if(browserType != null) {
				
				switch (EBrowsers.valueOf(browserType)) {

				case CHROME:
					client.killAllProcesses("chromedriver.exe", 1000);
					break;

				case INTERNET_EXPLORER:
					client.killAllProcesses("IEDriverServer.exe", 1000);
					break;

				case FIREFOX:
					client.killAllProcesses("geckodriver.exe", 1000);
					break;

				default:
					break;

				}
			}

		} catch (Exception e) { // NOPMD
		// Ignore exception
		}

	}


	/**
	 * Last action of the robot.
	 */
	public void end()  {
		server.info("End process");
	}
	
}
