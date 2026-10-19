package com.lalitha.sweets.service;


import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.layout.element.Image;
import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.lalitha.sweets.model.Order;
import com.lalitha.sweets.model.OrderItem;



@Service
public class InvoiceService {

	@Value("${app.frontend.base-url}")
	private String frontendBaseUrl;

	public byte[] generateInvoice(Order order) {

	    ByteArrayOutputStream out = new ByteArrayOutputStream();

	    PdfWriter writer = new PdfWriter(out);
	    PdfDocument pdf = new PdfDocument(writer);
	    

	 // =========================
	 // 🔥 LOGO WATERMARK (FINAL)
	 // =========================
	 PdfPage page = pdf.addNewPage(); // create page FIRST
	 Document document = new Document(pdf);
	 
	 PdfCanvas pdfCanvas = new PdfCanvas(page);

	 // transparency
	 PdfExtGState gs = new PdfExtGState();
	 gs.setFillOpacity(0.07f); // 👈 lighter = better

	 pdfCanvas.saveState();
	 pdfCanvas.setExtGState(gs);

	 try {
	     ImageData logoData = ImageDataFactory.create(
	         "https://uniim1.shutterfly.com/render/00-FBijLR0YHN1Ebjk4wvLi1Gtt577Dtj-Ks_LAQP6H1QP4HqBNgVRMOz5tiIaK_Fx55cYdfB3jRoL_VZILaXEufg?cn=THISLIFE&res=medium&ts=1761830717"
	     );

	     Image watermarkLogo = new Image(logoData);

	     // 🔥 FULL PAGE STYLE
	     watermarkLogo.scaleToFit(450, 450);
	     watermarkLogo.setFixedPosition(70, 200); // adjust if needed

	     Canvas canvas = new Canvas(pdfCanvas, page.getPageSize());
	     canvas.add(watermarkLogo);
	     canvas.close();

	 } catch (Exception e) {
	     System.out.println("Watermark logo failed");
	 }

	 pdfCanvas.restoreState();

	    
	// =========================
	// BUSINESS HEADER
	// =========================
	document.add(new Paragraph("LALITHA SURYA SWEETS")
	        .setBold()
	        .setFontSize(22)
	        .setTextAlignment(TextAlignment.CENTER));

	document.add(new Paragraph("Premium Traditional Sweets")
	        .setItalic()
	        .setFontSize(11)
	        .setTextAlignment(TextAlignment.CENTER));

	document.add(new Paragraph("Near Ganganamma temple center, Jangareddigudem, Andhra Pradesh 534447")
	        .setFontSize(10)
	        .setTextAlignment(TextAlignment.CENTER));

	document.add(new Paragraph("Phone: +91 9441752361")
	        .setFontSize(10)
	        .setTextAlignment(TextAlignment.CENTER));

	// Replace with actual GSTIN
	document.add(new Paragraph("GSTIN: 37BCFPA6431B1Z7")
	        .setBold()
	        .setFontSize(10)
	        .setTextAlignment(TextAlignment.CENTER));

	document.add(new Paragraph(
	        "____________________________________________________________________")
	        .setTextAlignment(TextAlignment.CENTER));


	// =========================
	// BILL OF SUPPLY TITLE
	// =========================
	document.add(new Paragraph("BILL OF SUPPLY")
	        .setBold()
	        .setFontSize(16)
	        .setTextAlignment(TextAlignment.CENTER));

	document.add(new Paragraph("\n"));


	// =========================
	// INVOICE INFORMATION
	// =========================
	String invoiceNumber = String.format("INV-%06d", order.getId());

	java.time.format.DateTimeFormatter formatter =
	        java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

	String formattedDate = order.getOrderDate().format(formatter);

	document.add(new Paragraph("Invoice No      : " + invoiceNumber));
	document.add(new Paragraph("Order ID        : " + order.getId()));
	document.add(new Paragraph("Invoice Date    : " + formattedDate));
	document.add(new Paragraph("Payment Status  : " +
	        (order.getPaymentStatus() != null
	                ? order.getPaymentStatus()
	                : "PENDING")));

	document.add(new Paragraph(
	        "____________________________________________________________________"));


	// =========================
	// CUSTOMER INFORMATION
	// =========================
	document.add(new Paragraph("Bill To:")
	        .setBold()
	        .setFontSize(12));

	document.add(new Paragraph(
	        order.getCustomerNameSnapshot() != null
	                ? order.getCustomerNameSnapshot()
	                : "Customer"));

	document.add(new Paragraph(
	        order.getCustomerPhoneSnapshot() != null
	                ? "+" + order.getCustomerPhoneSnapshot()
	                : "N/A"));

	document.add(new Paragraph(
	        order.getAddress() != null
	                ? order.getAddress()
	                : "Address not available"));

	document.add(new Paragraph(
	        "____________________________________________________________________"));

	document.add(new Paragraph("\n"));


	// =========================
		    // TABLE
		    // =========================
		    Table table = new Table(new float[]{3, 2, 1, 2});
		    table.setWidth(UnitValue.createPercentValue(100));

		    table.addHeaderCell("Product");
		    table.addHeaderCell("Price");
		    table.addHeaderCell("Qty");
		    table.addHeaderCell("Total");

		    for (OrderItem item : order.getItems()) {

		        BigDecimal total =
		                item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));

		        table.addCell(item.getProductName());
		        table.addCell("₹ " + item.getPrice());
		        table.addCell(String.valueOf(item.getQuantity()));
		        table.addCell("₹ " + total);
		    }

		    document.add(table);

	document.add(new Paragraph("\n"));


	// =========================
	// PAYMENT SUMMARY
	// =========================
	document.add(new Paragraph(
	        "____________________________________________________________________"));

	document.add(new Paragraph("PAYMENT SUMMARY")
	        .setBold()
	        .setFontSize(12));

	document.add(new Paragraph("Subtotal: ₹ " + order.getSubtotal()));
	document.add(new Paragraph("Handling Charges: ₹ " + order.getHandlingCharges()));
	document.add(new Paragraph("Delivery Charges: ₹ " + order.getDeliveryCharges()));

	document.add(new Paragraph(
	        "------------------------------------------------------------"));

	document.add(new Paragraph("Grand Total: ₹ " + order.getTotalAmount())
	        .setBold()
	        .setFontSize(14));

	document.add(new Paragraph(
	        "Composition taxable person, not eligible to collect tax on supplies.")
	        .setItalic()
	        .setFontSize(9));

	document.add(new Paragraph("\n"));


	// =========================
	// TRANSACTION DETAILS
	// =========================
	document.add(new Paragraph("TRANSACTION DETAILS")
	        .setBold()
	        .setFontSize(12));

	document.add(new Paragraph("Payment Method: Online Payment (Razorpay)"));

	document.add(new Paragraph("Payment ID: " +
	        (order.getPaymentId() != null
	                ? order.getPaymentId()
	                : "N/A")));

	document.add(new Paragraph("Razorpay Order ID: " +
	        (order.getRazorpayOrderId() != null
	                ? order.getRazorpayOrderId()
	                : "N/A")));

	document.add(new Paragraph("\n"));


	// =========================
	// QR CODE SECTION
	// =========================
	document.add(new Paragraph("Scan to Track Your Order")
	        .setBold()
	        .setTextAlignment(TextAlignment.CENTER));

	try {
	    Image qr = generateQRCode(order.getId());
	    qr.setHorizontalAlignment(HorizontalAlignment.CENTER);
	    document.add(qr);
	} catch (Exception e) {
	    e.printStackTrace();
	}

	document.add(new Paragraph("\n"));


	// =========================
	// PROFESSIONAL FOOTER
	// =========================
	document.add(new Paragraph(
	        "Thank you for shopping with Lalitha Surya Sweets!")
	        .setBold()
	        .setTextAlignment(TextAlignment.CENTER));

	document.add(new Paragraph(
	        "This is a computer-generated invoice and does not require a signature.")
	        .setItalic()
	        .setFontSize(9)
	        .setTextAlignment(TextAlignment.CENTER));

	 

	    document.close();
	    return out.toByteArray();
	}

	
	private Image generateQRCode(Long orderId) throws Exception {

	    String url = frontendBaseUrl + "/track/" + orderId;

	    QRCodeWriter writer = new QRCodeWriter();
	    BitMatrix bitMatrix = writer.encode(url, BarcodeFormat.QR_CODE, 200, 200);

	    ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
	    MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);

	    Image qrImage = new Image(
	            ImageDataFactory.create(pngOutputStream.toByteArray())
	    );

	    qrImage.setWidth(120);
	    return qrImage;
	}
}
