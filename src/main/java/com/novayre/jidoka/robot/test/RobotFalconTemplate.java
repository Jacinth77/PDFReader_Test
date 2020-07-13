


package com.novayre.jidoka.robot.test;


import com.novayre.jidoka.client.api.IJidokaServer;
import com.novayre.jidoka.client.api.IRobot;
import com.novayre.jidoka.client.api.JidokaFactory;
import com.novayre.jidoka.client.api.annotations.Robot;
import com.novayre.jidoka.client.api.multios.IClient;
import com.novayre.jidoka.falcon.api.*;
import com.novayre.jidoka.falcon.ocr.api.*;
import net.sourceforge.tess4j.Tesseract;
import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


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
		public void convertPdfToImage() throws  Exception{

			Path screenshot	 = Paths.get(server.getCurrentDir(), "screenshot.pdf");
			server.info("Path"+  screenshot);
			//String sourceDir ="C:\\Users\\prasanthraja.c\\Downloads\\screenshot.pdf";
		String sourceDir =screenshot.toString();
			String destinationDir = "C:\\Users\\prasanthraja.c\\Downloads\\Converted_PdfFiles_to_Image/"; // converted images from pdf document are saved here

			File sourceFile = new File(sourceDir);

			server.info("Passed sourceDir");
			File destinationFile = new File(destinationDir);
			if (!destinationFile.exists()) {
				destinationFile.mkdir();
				server.info("Folder Created -> "+ destinationFile.getAbsolutePath());
			}
			if (sourceFile.exists()) {
				server.info("Images copied to Folder Location: "+ destinationFile.getAbsolutePath());
				PDDocument document = PDDocument.load(sourceFile);
				PDFRenderer pdfRenderer = new PDFRenderer(document);

				int numberOfPages = document.getNumberOfPages();
				server.info("Total files to be converting -> "+ numberOfPages);

				String fileName = sourceFile.getName().replace(".pdf", "");
				String fileExtension= "png";

				int dpi = 300;// use less dpi for to save more space in harddisk. For professional usage you can use more than 300dpi

				for (int i = 0; i < numberOfPages; ++i) {
					File outPutFile = new File(destinationDir + fileName +"_"+ (i+1) +"."+ fileExtension);
					BufferedImage bImage = pdfRenderer.renderImageWithDPI(i, dpi, ImageType.RGB);
					ImageIO.write(bImage, fileExtension, outPutFile);

				}

				document.close();
				server.info("Converted Images are saved at -> "+ destinationFile.getAbsolutePath());
			} else {
				server.info(sourceFile.getName() +" File not exists");
			}



	}


public void extractTextFromOCR () throws Exception{
		String inputFile ="C:\\Users\\prasanthraja.c\\Downloads\\figure-651.png";
		Tesseract tesseract = new Tesseract();
		tesseract.setDatapath("D:\\IntelJWorkspace\\RPA Projects\\tessdata");
		tesseract.setLanguage("deu");
		String ExtractedText = tesseract.doOCR(new File(inputFile));
		server.info("Text :" + ExtractedText);
}


	/**
	 * End.
	 */
	public void end() {
		
	}
}
