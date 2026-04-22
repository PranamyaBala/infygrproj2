package com.hostel.booking.service;

import com.hostel.booking.entity.Booking;
import com.hostel.room.dto.RoomDTO;
import com.hostel.user.dto.UserDTO;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
public class ReceiptService {

    public byte[] generateReceipt(Booking booking, RoomDTO room, UserDTO user) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        
        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Font styles
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Font.BOLD);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            // Title
            Paragraph title = new Paragraph("BOOKING RECEIPT", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(30);
            document.add(title);

            // Reference Info
            Paragraph subtitle = new Paragraph("Reference: " + booking.getBookingReference(), headerFont);
            subtitle.setSpacingAfter(20);
            document.add(subtitle);

            // Table for Details
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            table.setSpacingAfter(20);

            addTableCell(table, "Student Name", user.getFirstName() + " " + user.getLastName(), headerFont, normalFont);
            addTableCell(table, "Student Email", user.getEmail(), headerFont, normalFont);
            addTableCell(table, "Room Number", room.getRoomNumber(), headerFont, normalFont);
            addTableCell(table, "Room Type", room.getRoomType(), headerFont, normalFont);
            addTableCell(table, "Check-in Date", booking.getStartDate().toString(), headerFont, normalFont);
            addTableCell(table, "Check-out Date", booking.getEndDate().toString(), headerFont, normalFont);
            addTableCell(table, "Occupants", String.valueOf(booking.getOccupants()), headerFont, normalFont);
            
            document.add(table);

            // Price Details
            Paragraph priceLabel = new Paragraph("Payment Information", headerFont);
            priceLabel.setSpacingBefore(20);
            document.add(priceLabel);

            PdfPTable priceTable = new PdfPTable(2);
            priceTable.setWidthPercentage(40);
            priceTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            
            BigDecimal totalPrice = booking.getTotalPrice().add(booking.getLateCheckoutFee() != null ? booking.getLateCheckoutFee() : BigDecimal.ZERO);
            
            addTableCell(priceTable, "Base Total", "INR " + booking.getTotalPrice(), headerFont, normalFont);
            if (booking.getLateCheckoutFee() != null && booking.getLateCheckoutFee().compareTo(BigDecimal.ZERO) > 0) {
                addTableCell(priceTable, "Late Checkout Fee", "INR " + booking.getLateCheckoutFee(), headerFont, normalFont);
            }
            addTableCell(priceTable, "GRAND TOTAL", "INR " + totalPrice, titleFont, titleFont);
            
            document.add(priceTable);

            // Footer
            Paragraph footer = new Paragraph("\n\nThank you for choosing our hostel! Please keep this receipt for your records.", smallFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Error generating PDF receipt", e);
        }

        return baos.toByteArray();
    }

    private void addTableCell(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell cell1 = new PdfPCell(new Phrase(label, labelFont));
        cell1.setPadding(8);
        cell1.setBorderColor(java.awt.Color.LIGHT_GRAY);
        table.addCell(cell1);

        PdfPCell cell2 = new PdfPCell(new Phrase(value, valueFont));
        cell2.setPadding(8);
        cell2.setBorderColor(java.awt.Color.LIGHT_GRAY);
        table.addCell(cell2);
    }
}
