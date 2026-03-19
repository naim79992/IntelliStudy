package com.example.naim.service.impl;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class DocumentParserServiceImpl {

    public record ParsedDocument(String text, int pageCount) {}

    public ParsedDocument parsePdf(MultipartFile file) throws IOException {
        try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            int pages = doc.getNumberOfPages();
            
            // Basic cleaning
            text = text.replaceAll("\\r\\n|\\r", "\n")
                       .replaceAll("[ \\t]+", " ")
                       .strip();
            
            return new ParsedDocument(text, pages);
        }
    }
}
