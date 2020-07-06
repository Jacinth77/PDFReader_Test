


package com.novayre.jidoka.robot.test;


import com.novayre.jidoka.client.api.IJidokaServer;
import com.novayre.jidoka.client.api.IRobot;
import com.novayre.jidoka.client.api.JidokaFactory;
import com.novayre.jidoka.client.api.annotations.Robot;
import com.novayre.jidoka.client.api.multios.IClient;
import com.novayre.jidoka.falcon.api.*;
import com.novayre.jidoka.falcon.ocr.api.*;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The Class RobotFalconTemplate.
 */
@Robot
public class RobotFalconTemplate implements IRobot {

	/** The server. */
	private IJidokaServer< ? > server;
	
	/** The falcon. */
	private IFalcon falcon;
	
	/** The client. */
	private IClient client;
	
	private JidokaImageSupport images;
	private RobotFalconTemplate MyRobot;


	/**
	 * Initialize the modules
	 * @throws IOException 
	 */
	public void start() throws IOException {
		
		server = JidokaFactory.getServer();
		client = IClient.getInstance(this);
		falcon = IFalcon.getInstance(this, client);
		images = JidokaImageSupport.getInstance(this);
		IFalconProcess falconProcess = falcon.getFalconProcess();

		
		server.setNumberOfItems(1);
		server.setCurrentItem(1, images.getTestPng().getDescription());
	}
	
	/**
	 * Search image.
	 * @throws IOException 
	 * @throws AWTException 
	 */
	public void searchImage() throws Exception {
		
		IFalconImage testImage = images.getTestPng().search();
		
		// Uncomment to apply 25% noise and color tolerance 
//		testImage.setTolerance(.25f);
//		testImage.getOptions().colorTolerance(.25f);
		
		server.info("Searching the image: " + testImage.getDescription());
		
		// Sends the image to the trace
		falcon.sendImage(testImage.getImage(), "Test image");

		
		server.info("Desktop capture");
		server.sendScreen("Current desktop");
		
		// Image search on the desktop
		if (testImage.search().found()) {

			
			server.info("Image found at: " + testImage.getPointsWhereFound().get(0));

			// Draw a rectangle where the image was found
			drawRectangle(testImage);
			
			// Single left click over the image
			testImage.clickOnCenter();
			
			server.setCurrentItemResultToOK();
			ReadOCR();
			
		} else {
			
			server.warn("Image not found");
			server.setCurrentItemResultToWarn();
		}
	}
	/*
	method for OCR
	 */

	public void ReadOCR() throws Exception{
		IFalconProcess falconProcess = falcon.getFalconProcess();
		Rectangle r = new Rectangle(0,3,276,51);
		OCROptions o = new OCROptions();
		o.setConfiguration(null);
		o.setLanguageInImage("eng");
		o.setTextFormInImage(1);

		Path screenshot = Paths.get(server.getCurrentDir(), "screenshot.png");
		BufferedImage img = ImageIO.read(new File(String.valueOf(screenshot)));
		server.info("Data  "+img.getData());
		server.info("Answer    "+falcon.extractText(img,r, ETextFormInImage.AUTOMATIC_PAGE_SEGMENTATION_WITH_OSD, ELanguageInImage.ENGLISH,null
				,1,1));

	}
	
	/**
	 * Draw rectangle over screenshot capture to show where the image was found
	 *
	 * @param imageFound the image found
	 * @return the string
	 * @throws AWTException 
	 */
	private String drawRectangle(IFalconImage imageFound) throws AWTException {

		try {
			
			Path screenshot = Paths.get(server.getCurrentDir(), "screenshot.png");
			ImageIO.write(server.getScreen(), "png", screenshot.toFile());
			
			BufferedImage img = ImageIO.read(screenshot.toFile());
			Graphics2D g2d = img.createGraphics();

			Point p = imageFound.getPointsWhereFound().get(0);
			server.info("Point  +"+imageFound.getPointsWhereFound().get(0));

			Rectangle r = new Rectangle(529,239,1366,768);
			OCROptions o = new OCROptions();
			o.setConfiguration(null);
			o.setLanguageInImage("eng");
			o.setTextFormInImage(8);

			Path screenshot1 = Paths.get(server.getCurrentDir(), "screenshot.png");
			BufferedImage img1 = ImageIO.read(new File(String.valueOf(screenshot1)));
			server.info("Data  "+img.getData());
			server.info("Answer    "+falcon.extractText(img1,r, ETextFormInImage.AUTOMATIC_PAGE_SEGMENTATION_WITH_OSD, ELanguageInImage.ENGLISH,null
					,1,1));
			server.info("Answer2  "+falcon.extractTextImageProcessing(img1,r,o));




			g2d.setStroke(new BasicStroke(3));
			g2d.setColor(Color.RED);
			g2d.drawRect(p.x, p.y, imageFound.getRectangle().width, imageFound.getRectangle().height);
			g2d.dispose();

			String output = addSuffix(screenshot.toFile().getAbsolutePath(), "_mod");

			File f = new File(output);

			ImageIO.write(img, "png", f);
			server.info("Save image modified: " + output);
			
			falcon.sendImage(ImageIO.read(new File(output)), output);

			return output;
		} catch (IOException | InterruptedException e) {
			return null;
		}

	}

	public void recognize() throws Exception {
		Path screenshot = Paths.get(server.getCurrentDir(), "screenshot_mod.png");
		BufferedImage defaultImage =
				ImageIO.read(Paths.get(server.getCurrentDir(),  "screenshot_mod.png").toFile());

		if (defaultImage != null){
			Rectangle rectanguloCif = new Rectangle(defaultImage.getWidth(), defaultImage.getHeight());


			Graphics2D g2d = defaultImage.createGraphics();
			IFalconProcess falconProcess = falcon.getFalconProcess();
			StartParameters s = new StartParameters();
			s.setLogImages(true);
			s.setLogIntermediateMessages(true);
			s.setImageDescription("Original");

			falconProcess.start(defaultImage,s);

			falconProcess.ocr(new OCRParameters()
							.languageInImage("eng")
							.configuration(null)
							.textFormInImage(1));

			String output = addSuffix(screenshot.toFile().getAbsolutePath(), "_modnew");
			File f = new File(output);
			ImageIO.write(defaultImage, "png", f);
			server.info("Save image modified: " + output);
			falcon.sendImage(ImageIO.read(new File(output)), output);




			//falconProcess.(new ThresholdParameters().thresh(100).maxVal(255).otsu(false).type(ThresholdParameters.EType.BINARY));
			//String text = falcon.extractText(defaultImage, rectanguloCif, ETextFormInImage.FULLY_AUTOMATIC_PAGE_SEGMENTATION_WITHOUT_OSD,
			//		ELanguageInImage.ENGLISH, null, 1.9f, 0f);


			//server.info("Resutl:" + text);

		}else{
			server.info("Null Image");
		}
	}

	/*public static BufferedImage convertToARGB(BufferedImage image)
	{

		BufferedImage img = image.getSubimage(0, 0, 58, 15);
		BufferedImage copyOfImage = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = copyOfImage.createGraphics();
		RenderingHints rh = new RenderingHints(
				RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		g.setRenderingHints(rh);
		g.drawImage(img, 0, 0, null);
		return copyOfImage;

	}
	*/
	public static BufferedImage convertToARGB(BufferedImage image)
	{
		BufferedImage newImage = new BufferedImage(
				image.getWidth(), image.getHeight(),
				BufferedImage.TYPE_INT_ARGB);


		Graphics2D g = newImage.createGraphics();
		RenderingHints rh = new RenderingHints(
				RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		g.setRenderingHints(rh);

		g.drawImage(image, 0, 1, null);
		g.dispose();

		return newImage;

	}
	
	/**
	 * Adds the suffix to a filename
	 *
	 * @param filename the filename
	 * @param suffix the suffix
	 * @return the string
	 */
	private String addSuffix(String filename, String suffix) {

		String baseName = FilenameUtils.getBaseName(filename);
		baseName += suffix;

		String output = FilenameUtils.getFullPath(filename) + File.separator + baseName + "."
				+ FilenameUtils.getExtension(filename);

		return output;
	}
	
	/**
	 * End.
	 */
	public void end() {
		
	}
}
