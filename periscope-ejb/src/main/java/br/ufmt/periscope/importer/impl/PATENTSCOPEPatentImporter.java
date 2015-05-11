package br.ufmt.periscope.importer.impl;

import br.ufmt.periscope.enumerated.ClassificationType;
import br.ufmt.periscope.importer.PatentImporter;
import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.model.Classification;
import br.ufmt.periscope.model.Inventor;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.repository.CountryRepository;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

public class PATENTSCOPEPatentImporter implements PatentImporter {

    private @Inject
    CountryRepository countryRepository;
    private final String lang = "EN";
    private HSSFWorkbook wb;
    private HSSFSheet sheet;
    private Row row;
    private Iterator<Row> rowIterator;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
    private Patent patent;

    @Override
    public boolean initWithStream(InputStream is) {
        try {
            wb = new HSSFWorkbook(is);
            sheet = wb.getSheetAt(0);

            rowIterator = sheet.iterator();

            //Pulando primeiras linhas
            rowIterator.next(); //Linha em branco
            rowIterator.next(); //Consulta

        } catch (IOException ex) {
            //Se entrar aqui é porque o arquivo não está no padrão
            Logger.getLogger(PATENTSCOPEPatentImporter.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }

    @Override
    public boolean hasNext() {
        return rowIterator.hasNext();
    }

    @Override
    public Patent next() {
        patent = new Patent();
        parseLine();
        return patent;
    }

    @Override
    public void remove() {
    }

    private void parseLine() {

        patent.setLanguage(lang);

        row = rowIterator.next(); //Percorrendo cada linha (patente)
        // Para cada linha (patente), pega cada atributo (Titulo, Publicação, Autor ...)
        Iterator<Cell> cellIterator = row.cellIterator();
        while (cellIterator.hasNext()) {

            Cell cell = cellIterator.next(); // Pegando cada coluna (atributo)

            switch (cell.getCellType()) {

                case Cell.CELL_TYPE_STRING:
                    fillPatent(cell.getColumnIndex(), cell.getStringCellValue());
                    break;
                default:
                    break;
            }

        }
    }

    private void fillPatent(int columnIndex, String contentString) {

        switch (columnIndex) {
            case 0:
                patent.setPublicationCountry(countryRepository.getCountryByAcronym(contentString.substring(0, 2)));
                patent.setPublicationNumber(contentString);
                break;
            case 1:
                try {
                    patent.setPublicationDate(sdf.parse(contentString.trim()));
                } catch (ParseException ex) {
                    Logger.getLogger(PATENTSCOPEPatentImporter.class.getName()).log(Level.SEVERE, null, ex);
                }
                break;
            case 2:
                patent.setTitleSelect(contentString.trim());
                break;
            case 3:
                patent.setAbstractSelect(contentString.trim());
                break;
            case 4:
                StringTokenizer st = new StringTokenizer(contentString, ";");
                List<Classification> classifications = patent.getClassifications();
                while (st.hasMoreTokens()) {
                    classifications.add(new Classification(st.nextToken().trim(), ClassificationType.IC));
                }
                patent.setClassifications(classifications);
                break;
            case 5:
                st = new StringTokenizer(contentString, ";");
                List<Applicant> applicants = patent.getApplicants();
                while (st.hasMoreTokens()) {
                    applicants.add(new Applicant(st.nextToken().trim()));
                }
                patent.setApplicants(applicants);
                break;
            case 6:
                st = new StringTokenizer(contentString, ";");
                List<Inventor> inventors = patent.getInventors();
                while (st.hasMoreTokens()) {
                    inventors.add(new Inventor(st.nextToken().trim()));
                }
                patent.setInventors(inventors);
                break;
        }

    }

    @Override
    public String provider() {
        return "PATENTSCOPE";
    }
}
