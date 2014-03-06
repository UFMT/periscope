package br.ufmt.periscope.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.util.PDFTextStripper;

public class PDFTextParser {
    PDFParser parser;
    String parsedText;
    PDFTextStripper pdfStripper;
    PDDocument pdDoc;
    COSDocument cosDoc;
    PDDocumentInformation pdInformation;
    
    String pdfToText(String fileName) throws IOException {
        File file = new File(fileName);
        try {
            parser = new PDFParser(new FileInputStream(file));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PDFTextParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        parser.parse();
        cosDoc = parser.getDocument();
        pdfStripper = new PDFTextStripper();
        pdDoc = new PDDocument(cosDoc);
        parsedText = pdfStripper.getText(pdDoc);
        
        return parsedText;
    }
    
    void writeTextToFile(String pdfText, String fileName) {
        
        
        try {
            PrintWriter pw = new PrintWriter(fileName);
            pw.print(pdfText);
            pw.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PDFTextParser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
