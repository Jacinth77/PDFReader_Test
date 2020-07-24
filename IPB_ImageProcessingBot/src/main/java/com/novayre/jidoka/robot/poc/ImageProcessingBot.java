package com.novayre.jidoka.robot.poc;

import java.awt.AWTException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;
import javax.imageio.ImageIO;
import com.google.gson.Gson;
import com.novayre.jidoka.client.api.appian.IAppian;
import com.novayre.jidoka.client.api.appian.webapi.IWebApiRequest;
import com.novayre.jidoka.client.api.appian.webapi.IWebApiRequestBuilderFactory;
import com.novayre.jidoka.client.api.appian.webapi.IWebApiResponse;
import com.novayre.jidoka.data.provider.api.EExcelType;
import com.novayre.jidoka.data.provider.api.IExcel;
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

/**
 * The Class ImageProcessingBot.
 */
@Robot
public class ImageProcessingBot implements IRobot {

	/** The server. */
	private IJidokaServer< ? > server;
	/** The falcon. */
	private IFalcon falcon;
	/** The client. */
	private IClient client;
	private JidokaImageSupport images;
    private IExcel excel;
    private String robotDir;
    private String excelName;
    private String docType ="PASSPORT";
	private HashMap<String, String> results = new HashMap<>();
	private String[] ocrValues = new String[]{
			"123456",
			docType,
			"John",
			"Smith",
			"22/04/1985",
			"Europe"
	};



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
		Path inputFile = Paths.get(server.getCurrentDir(), "Dutch.pdf");
		String fileType = FilenameUtils.getExtension(inputFile.toString());
		server.info("fileType " + fileType);
		String expMsg = "File Error";
		if(fileType !=null) {
			if (fileType.contains("pdf")) {
				return "Yes";

			}
			else {
				return "No";
			}
		}
		return expMsg;
	}
	public void convertPdfToImage() throws  Exception{

		Path screenshot	 = Paths.get(server.getCurrentDir(), "Dutch.pdf");
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
				row1.createCell(0, CellType.STRING).setCellValue(ocrValues[0]);
				row1.createCell(1, CellType.STRING).setCellValue(ocrValues[1]);
				row1.createCell(2, CellType.STRING).setCellValue(ocrValues[2]);

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
				row1.createCell(0, CellType.STRING).setCellValue(ocrValues[0]);
				row1.createCell(1, CellType.STRING).setCellValue(ocrValues[1]);
				row1.createCell(2, CellType.STRING).setCellValue(ocrValues[2]);

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
				row1.createCell(0, CellType.STRING).setCellValue(ocrValues[0]);
				row1.createCell(1, CellType.STRING).setCellValue(ocrValues[1]);
				row1.createCell(2, CellType.STRING).setCellValue(ocrValues[2]);

// Define the column width

				excel.getSheet().setDefaultColumnWidth(20);
				//excel.getSheet().autoSizeColumn(0);
			}

		}
		catch (Exception e){
			server.error(e);
		}
// Row content
		server.info(ocrValues.length);

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
		results.put("ExcelDocID ",output);
		results.put("IDNumber",ocrValues[0]);
		results.put("DocType ",ocrValues[1]);
		results.put("FirstName",ocrValues[2]);
		results.put("LastName",ocrValues[3]);
		results.put("DOB",ocrValues[4]);
		results.put("Place",ocrValues[5]);
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

		public void ImgPreprocessing() throws Exception{


	}
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

			return  new String[0] ;
		}

		}

