package com.novayre.jidoka.robot.poc;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.HashMap;
import java.util.*;
import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.FileImageOutputStream;

import com.google.gson.Gson;
import com.novayre.jidoka.client.api.appian.IAppian;
import com.novayre.jidoka.client.api.appian.webapi.IWebApiRequest;
import com.novayre.jidoka.client.api.appian.webapi.IWebApiRequestBuilderFactory;
import com.novayre.jidoka.client.api.appian.webapi.IWebApiResponse;
import com.novayre.jidoka.data.provider.api.EExcelType;
import com.novayre.jidoka.data.provider.api.IExcel;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import com.novayre.jidoka.client.api.IJidokaServer;
import com.novayre.jidoka.client.api.IRobot;
import com.novayre.jidoka.client.api.JidokaFactory;
import com.novayre.jidoka.client.api.annotations.Robot;
import com.novayre.jidoka.client.api.multios.IClient;
import com.novayre.jidoka.falcon.api.IFalcon;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.w3c.dom.Element;
import net.sourceforge.tess4j.Tesseract;
import java.util.List;

/**
 * The Class ImageProcessingBot.
 */
@Robot
public class ImageProcessingBot implements IRobot {

	/** The server. */
	private IJidokaServer< ? > server;
	public String fileNameinput;
	/** The falcon. */
	private IFalcon falcon;
	/** The client. */
	private IClient client;
	private JidokaImageSupport images;
    private IExcel excel;
    private String robotDir;
    private String excelName;
    private String docType ="";
	private HashMap<String, String> results = new HashMap<>();
	/*private String[] ocrValues = new String[]{
			"123456",
			docType,
			"John",
			"Smith",
			"22/04/1985",
			"Europe"
	};*/

	private  List<String> ocrValues = new ArrayList<String>();



	/**
	 * Initialize the modules
	 * @throws IOException
	 */
	public void start() throws IOException {
		server = JidokaFactory.getServer();
		client = IClient.getInstance(this);
		falcon = IFalcon.getInstance(this, client);
		images = JidokaImageSupport.getInstance(this);
		excel = IExcel.getExcelInstance(this);
		robotDir = server.getCurrentDir();
		server.setNumberOfItems(1);
		server.setCurrentItem(1, images.getTestPng().getDescription());
	}

	/**
	 * check file extension
	 * @throws IOException
	 * @throws AWTException
	 */
	public String isPDF() throws Exception {

		fileNameinput = server.getParameters().get("docFile");
		Path inputFile = Paths.get(server.getCurrentDir(), fileNameinput);
		String fileType = FilenameUtils.getExtension(inputFile.toString());
		server.info("fileType " + fileType);
		String expMsg = "File Error";
		if(fileType !=null) {
			if (fileType.contains("pdf")) {
				return "Yes";

			}
			else {
				BufferedImage defaultImage =
						ImageIO.read(Paths.get(inputFile.toString()).toFile());
				ImageIO.write(defaultImage, "jpg", new File("D:\\Output file\\"+fileNameinput+ ".jpg"));
				return "No";
			}
		}
		return expMsg;
	}
	public void convertPdfToImage() throws  Exception{

		Path screenshot	 = Paths.get(server.getCurrentDir(), fileNameinput);

		//Path screenshot	 = Paths.get(server.getCurrentDir(), "Dutch.pdf");
		server.info("Path"+  screenshot);
		//String sourceDir ="C:\\Users\\prasanthraja.c\\Downloads\\screenshot.pdf";
		String sourceDir =screenshot.toString();
		//String destinationDir = "C:\\Users\\prasanthraja.c\\Downloads\\Converted_PdfFiles_to_Image/"; // converted images from pdf document are saved here
		String destinationDir = "D:\\Output file"; // converted images from pdf document are saved here
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
			String fileExtension= "jpg";

			int dpi = 300;

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
	public void writeToExcel() throws Exception{


		// Create a unique name for the Excel file
		excelName = String.valueOf(docType+new Date().getTime()) + ".xlsx";
		// Final path of the file
		File file = Paths.get(robotDir, excelName).toFile();
		String excelPath = file.getAbsolutePath();
		try(IExcel excel = IExcel.getExcelInstance(this)) {
			excel.create(excelPath, EExcelType.XLSX);
			boolean passport = excel.createSheet("Passport",0);
			server.info(passport);
			boolean dl = excel.createSheet("DL",1);
			boolean idc = excel.createSheet("Identity Card",2);
			if(docType.contains("PASSPORT") && passport) {
				// Create row
				Row row = excel.getWorkbook().getSheet("Passport").createRow(0);
				row.createCell(0, CellType.STRING).setCellValue("First Name");
				row.createCell(1, CellType.STRING).setCellValue("Last Name");
				row.createCell(2, CellType.STRING).setCellValue("Passport No");
				row.createCell(3, CellType.STRING).setCellValue("Date of Birth");
				row.createCell(4, CellType.STRING).setCellValue("Place");
				//row.createCell(3, CellType.STRING).setCellValue("Stock");
				// Create header row style
				XSSFCellStyle style = (XSSFCellStyle)
						excel.getWorkbook().createCellStyle();
				style.setAlignment(HorizontalAlignment.CENTER);
				style.setFillForegroundColor(new XSSFColor(new java.awt.Color(101, 166, 255)));
				style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

			// Create the header row font
				XSSFFont font = (XSSFFont) excel.getWorkbook().createFont();
				font.setColor(IndexedColors.WHITE.getIndex());
				font.setFontName("Verdana");
				font.setFontHeight(10);
				style.setFont(font);

			// Apply style
				row.getCell(0).setCellStyle(style);
				row.getCell(1).setCellStyle(style);
				row.getCell(2).setCellStyle(style);
				//row.getCell(3).setCellStyle(style);
				// Row content
				Row row1 = excel.getWorkbook().getSheet("Passport").createRow(1);
				row1.createCell(0, CellType.STRING).setCellValue(ocrValues.get(2));
				row1.createCell(1, CellType.STRING).setCellValue(ocrValues.get(3));
				row1.createCell(2, CellType.STRING).setCellValue(ocrValues.get(0));
				row1.createCell(3, CellType.STRING).setCellValue(ocrValues.get(4));
				row1.createCell(4, CellType.STRING).setCellValue(ocrValues.get(5));

// Define the column width

				excel.getSheet().setDefaultColumnWidth(20);
				//excel.getSheet().autoSizeColumn(0);
			}
			if(docType.contains("DRIVING_LICENSE") && dl) {
				// Create row
				Row row = excel.getWorkbook().getSheet("DL").createRow(0);
				row.createCell(0, CellType.STRING).setCellValue("First Name");
				row.createCell(1, CellType.STRING).setCellValue("Last Name");
				row.createCell(2, CellType.STRING).setCellValue("DL No");
				row.createCell(3, CellType.STRING).setCellValue("Date of Birth");
				row.createCell(4, CellType.STRING).setCellValue("Place");
				//row.createCell(3, CellType.STRING).setCellValue("Stock");
				// Create header row style
				XSSFCellStyle style = (XSSFCellStyle)
						excel.getWorkbook().createCellStyle();
				style.setAlignment(HorizontalAlignment.CENTER);
				style.setFillForegroundColor(new XSSFColor(new java.awt.Color(101, 166, 255)));
				style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

				// Create the header row font
				XSSFFont font = (XSSFFont) excel.getWorkbook().createFont();
				font.setColor(IndexedColors.WHITE.getIndex());
				font.setFontName("Verdana");
				font.setFontHeight(10);
				style.setFont(font);

				// Apply style
				row.getCell(0).setCellStyle(style);
				row.getCell(1).setCellStyle(style);
				row.getCell(2).setCellStyle(style);
				//row.getCell(3).setCellStyle(style);
				// Row content
				Row row1 = excel.getWorkbook().getSheet("DL").createRow(1);
				row1.createCell(0, CellType.STRING).setCellValue(ocrValues.get(2));
				row1.createCell(1, CellType.STRING).setCellValue(ocrValues.get(3));
				row1.createCell(2, CellType.STRING).setCellValue(ocrValues.get(0));
				row1.createCell(3, CellType.STRING).setCellValue(ocrValues.get(4));
				row1.createCell(4, CellType.STRING).setCellValue(ocrValues.get(5));

// Define the column width

				excel.getSheet().setDefaultColumnWidth(20);
				//excel.getSheet().autoSizeColumn(0);
			}

			if(docType.contains("IDENTITY_CARD") && idc) {
				// Create row
				Row row = excel.getWorkbook().getSheet("DL").createRow(0);
				row.createCell(0, CellType.STRING).setCellValue("First Name");
				row.createCell(1, CellType.STRING).setCellValue("Last Name");
				row.createCell(2, CellType.STRING).setCellValue("Id No");
				row.createCell(3, CellType.STRING).setCellValue("Date of Birth");
				row.createCell(4, CellType.STRING).setCellValue("Place");
				//row.createCell(3, CellType.STRING).setCellValue("Stock");
				// Create header row style
				XSSFCellStyle style = (XSSFCellStyle)
						excel.getWorkbook().createCellStyle();
				style.setAlignment(HorizontalAlignment.CENTER);
				style.setFillForegroundColor(new XSSFColor(new java.awt.Color(101, 166, 255)));
				style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

				// Create the header row font
				XSSFFont font = (XSSFFont) excel.getWorkbook().createFont();
				font.setColor(IndexedColors.WHITE.getIndex());
				font.setFontName("Verdana");
				font.setFontHeight(10);
				style.setFont(font);

				// Apply style
				row.getCell(0).setCellStyle(style);
				row.getCell(1).setCellStyle(style);
				row.getCell(2).setCellStyle(style);
				//row.getCell(3).setCellStyle(style);
				// Row content
				Row row1 = excel.getWorkbook().getSheet("Identity Card").createRow(1);
				row1.createCell(0, CellType.STRING).setCellValue(ocrValues.get(2));
				row1.createCell(1, CellType.STRING).setCellValue(ocrValues.get(3));
				row1.createCell(2, CellType.STRING).setCellValue(ocrValues.get(0));
				row1.createCell(3, CellType.STRING).setCellValue(ocrValues.get(4));
				row1.createCell(4, CellType.STRING).setCellValue(ocrValues.get(5));

// Define the column width

				excel.getSheet().setDefaultColumnWidth(20);
				//excel.getSheet().autoSizeColumn(0);
			}

		}
		catch (Exception e){
			server.error(e);
		}
// Row content
		server.info(ocrValues.size());

		}
	public void uploadAttachmentsToAppian() throws Exception {
		String robotDir = server.getCurrentDir();
		File attachmentsDir = new File(robotDir);
		server.debug("Looking for files in: " + attachmentsDir.getAbsolutePath());
		File[] filesToUpload = Objects.<File[]>requireNonNull(attachmentsDir.listFiles());
		server.setNumberOfItems(filesToUpload.length);
		String filename = attachmentsDir.getAbsolutePath() + "\\Documents available for " + docType + ".xls";
		File fileUpload = new File(filename);
		String output = uploadExcel(fileUpload);
		Gson gson =new Gson();
		//results.put("DocID", gson.toJson(results));
		results.put("ExcelDocID",output);
		results.put("IDNumber",ocrValues.get(0));
		results.put("DocType",ocrValues.get(1));
		results.put("FirstName",ocrValues.get(2));
		results.put("LastName",ocrValues.get(3));
		results.put("DOB",ocrValues.get(4));
		results.put("Place",ocrValues.get(5));
		server.info("Upload result " + results);
	}
	private String uploadExcel(File file) throws Exception{
		String endpointUpload = ((String)this.server.getEnvironmentVariables().get("endPointUpload")).toString();
		File uploadFile = Paths.get(robotDir, excelName).toFile();
		//String excelPath = uploadFile.getAbsolutePath();
		IAppian appianClient =IAppian.getInstance(this);
		IWebApiRequest request = IWebApiRequestBuilderFactory.getFreshInstance().uploadDocument(endpointUpload,uploadFile,uploadFile.getName()).build();
		String result = appianClient.callWebApi(request).getBodyString();
		String value = result.split(":")[1];
		String output = value.split(" -")[0];
		this.server.info("output:" + output);
		return output;
	}
//PreProcessing
		public void ImgPreprocessing() throws Exception{
			SetDPI();
			String DPI = "D:\\Output file\\DPI.jpg";
			BufferedImage defaultImage1 =
					ImageIO.read(Paths.get(DPI).toFile());

			BufferedImage defaultImage = new BufferedImage(1550, 1024, defaultImage1.getType());
			Graphics2D graphic = defaultImage.createGraphics();
			graphic.drawImage(defaultImage1, 0, 0, 1550, 1024, null);
			graphic.dispose();
			//ImageIO.write(defaultImage, "jpg", new File("D:\\Output file\\contrast"  + ".jpg"));

			Tesseract tesseract = new Tesseract();
			tesseract.setDatapath("D:\\TessData");
			tesseract.setLanguage("eng");


			BufferedImage dest = defaultImage.getSubimage(590, 425,200, 30);
			String Country = "D:\\Output file\\Country.jpg";
			ImageIO.write(dest, "jpg", new File(Country));
			String CountryName = tesseract.doOCR(new File(Country));
			server.info("Country :" + CountryName);



			BufferedImage first = defaultImage.getSubimage(570, 325,350, 35);
			String firstname = "D:\\Output file\\first.jpg";
			ImageIO.write(first, "jpg", new File(firstname));
			String firstnamevalue = tesseract.doOCR(new File(firstname));
			server.info("firstnamevalue :" + firstnamevalue);


			BufferedImage Last = defaultImage.getSubimage(570, 250,300, 35);
			String Lastname = "D:\\Output file\\Last.jpg";
			ImageIO.write(Last, "jpg", new File(Lastname));
			String Lastnamev = tesseract.doOCR(new File(Lastname));
			server.info("Lastnamev :" + Lastnamev);

			BufferedImage number = defaultImage.getSubimage(1120, 168,350, 65);
			String number1 = "D:\\Output file\\num.jpg";
			ImageIO.write(number, "jpg", new File(number1));
			String ID = tesseract.doOCR(new File(number1));
			server.info("ID :" + ID);
			if (ID.length()<11) {
				docType="PASSPORT";
			}

			BufferedImage DOB = defaultImage.getSubimage(1150, 393,350, 55);
			String DOB1 = "D:\\Output file\\dob.jpg";
			ImageIO.write(DOB, "jpg", new File(DOB1));
			String DOB2 = tesseract.doOCR(new File(DOB1));
			server.info("DOB :" + DOB2);

			BufferedImage place = defaultImage.getSubimage(670, 493,700, 65);
			String place1 = "D:\\Output file\\place.jpg";
			ImageIO.write(place, "jpg", new File(place1));
			String place2 = tesseract.doOCR(new File(place1));
			server.info("place :" + place2);

			ocrValues.add(0,ID);
			ocrValues.add(1,docType);
			ocrValues.add(2,firstnamevalue);
			ocrValues.add(3,Lastnamev);
			ocrValues.add(4,DOB2);
			ocrValues.add(5,place2);

			/*results.put("IDNumber",ID);
			results.put("docType",docType);
			results.put("firstName",firstnamevalue);
			results.put("lastName",Lastnamev);
			results.put("DOB",DOB2);
			results.put("place",place2);*/
	}

	public void SetDPI() throws IOException {

		String inputFilePath ="D:\\Output file\\"+fileNameinput+ ".jpg";
		// String inputFilePath = Paths.get(server.getCurrentDir(), fileNameinput).toString();
		BufferedImage image = ImageIO.read(new File(inputFilePath));
		ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
		ImageWriteParam param = writer.getDefaultWriteParam();
		param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		param.setCompressionQuality(0.95f);
		IIOMetadata metadata = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(image), param);
		Element tree = (Element) metadata.getAsTree("javax_imageio_jpeg_image_1.0");
		Element jfif = (Element) tree.getElementsByTagName("app0JFIF").item(0);
		jfif.setAttribute("Xdensity", Integer.toString(300));
		jfif.setAttribute("Ydensity", Integer.toString(300));
		jfif.setAttribute("resUnits", "1"); // In pixels-per-inch units
		metadata.mergeTree("javax_imageio_jpeg_image_1.0", tree);
		String filename = "D:\\Output file\\DPI.jpg";

		try (FileImageOutputStream output = new FileImageOutputStream(new File(filename))) {
			writer.setOutput(output);
			IIOImage iioImage = new IIOImage(image, null, metadata);
			writer.write(metadata, iioImage, param);
			writer.dispose();
		}
	}

	//preprocessing over
	public void recognizeText() throws Exception{


	}
	public void setData() throws Exception{


	}
	/**
		 * End.
		 */
		public void end() {
			server.setResultProperties(results);
		}

		public String[] cleanUp() throws Exception{
			// Constructs the execution ID to pass to the web API
			String executionId = server.getExecution(0).getRobotName() + "#" +
					server.getExecution(0).getCurrentExecution().getExecutionNumber();
			server.info(executionId);
			// Calls the notifyProcessOfCompletion web API and passes the execution ID
			IAppian appian = IAppian.getInstance(this);
			IWebApiRequest request = IWebApiRequestBuilderFactory.getFreshInstance()
					.post("send-RPA-message")
					.body(executionId)
					.build();
			server.info("Webapi-Request" + request.getQueryParameters());
			IWebApiResponse response = appian.callWebApi(request);

			// Displays the result of the web API in the execution log for easy debugging
			server.info("Response body: " + new String(response.getBodyBytes()));
			String directory= "D:\\Output file\\";
			FileUtils.cleanDirectory((new File(directory)));

			return  new String[0] ;
		}

		}

