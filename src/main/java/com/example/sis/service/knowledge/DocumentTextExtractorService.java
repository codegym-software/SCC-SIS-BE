package com.example.sis.service.knowledge;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Service for extracting text from various document formats
 * Supports: PDF, DOC, DOCX, TXT, MD
 */
@Service
@Slf4j
public class DocumentTextExtractorService {

    /**
     * Extract text from uploaded file based on content type
     * @param file MultipartFile to extract text from
     * @return Extracted text content
     * @throws IOException if file cannot be read or format not supported
     */
    public String extractText(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        
        log.info("📄 Extracting text from: {} (type: {})", filename, contentType);
        
        // Get file extension
        String extension = "";
        if (filename != null && filename.contains(".")) {
            extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        }
        
        // Extract based on content type or file extension
        try {
            if ("pdf".equals(extension) || "application/pdf".equals(contentType)) {
                return extractFromPdf(file.getInputStream());
            } else if ("docx".equals(extension) || 
                       "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType)) {
                return extractFromDocx(file.getInputStream());
            } else if ("doc".equals(extension) || 
                       "application/msword".equals(contentType)) {
                return extractFromDoc(file.getInputStream());
            } else if ("txt".equals(extension) || 
                       "md".equals(extension) || 
                       "text/plain".equals(contentType) || 
                       "text/markdown".equals(contentType)) {
                return new String(file.getBytes(), StandardCharsets.UTF_8);
            } else {
                throw new IOException("Unsupported file format: " + contentType + " (" + extension + ")");
            }
        } catch (Exception e) {
            log.error("❌ Failed to extract text from {}: {}", filename, e.getMessage(), e);
            throw new IOException("Failed to extract text: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extract text from PDF file
     */
    private String extractFromPdf(InputStream inputStream) throws IOException {
        log.info("📕 Extracting from PDF...");
        
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            
            log.info("✅ PDF extracted: {} characters", text.length());
            return text;
        }
    }
    
    /**
     * Extract text from DOCX file (Word 2007+)
     */
    private String extractFromDocx(InputStream inputStream) throws IOException {
        log.info("📘 Extracting from DOCX...");
        
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder text = new StringBuilder();
            
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                text.append(paragraph.getText()).append("\n");
            }
            
            log.info("✅ DOCX extracted: {} characters", text.length());
            return text.toString();
        }
    }
    
    /**
     * Extract text from DOC file (Word 97-2003)
     */
    private String extractFromDoc(InputStream inputStream) throws IOException {
        log.info("📙 Extracting from DOC...");
        
        try (HWPFDocument document = new HWPFDocument(inputStream)) {
            WordExtractor extractor = new WordExtractor(document);
            String text = extractor.getText();
            
            log.info("✅ DOC extracted: {} characters", text.length());
            return text;
        }
    }
    
    /**
     * Check if file format is supported
     */
    public boolean isSupported(String filename) {
        if (filename == null) return false;
        
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return extension.matches("(pdf|doc|docx|txt|md)");
    }
}
