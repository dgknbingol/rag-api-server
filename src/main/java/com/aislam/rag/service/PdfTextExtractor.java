package com.aislam.rag.service;

import com.aislam.rag.domain.PdfPageText;
import com.aislam.rag.exception.RagException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class PdfTextExtractor {

    public List<PdfPageText> extractPages(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new RagException("PDF file is empty");
        }

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            int totalPages = document.getNumberOfPages();
            List<PdfPageText> pages = new ArrayList<>(totalPages);

            for (int pageNumber = 1; pageNumber <= totalPages; pageNumber++) {
                stripper.setStartPage(pageNumber);
                stripper.setEndPage(pageNumber);
                pages.add(new PdfPageText(pageNumber, stripper.getText(document)));
            }

            return List.copyOf(pages);
        } catch (IOException ex) {
            throw new RagException("Failed to read PDF: " + ex.getMessage(), ex);
        }
    }
}
