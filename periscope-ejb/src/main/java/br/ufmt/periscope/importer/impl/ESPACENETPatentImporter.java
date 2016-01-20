package br.ufmt.periscope.importer.impl;

import br.ufmt.periscope.enumerated.ClassificationType;
import br.ufmt.periscope.importer.PatentImporter;
import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.model.Classification;
import br.ufmt.periscope.model.Country;
import br.ufmt.periscope.model.History;
import br.ufmt.periscope.model.Inventor;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Priority;
import br.ufmt.periscope.repository.CountryRepository;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

public class ESPACENETPatentImporter implements PatentImporter {

    private @Inject
    CountryRepository countryRepository;
    private String lang = "EN";
    private HSSFWorkbook wb;
    private HSSFSheet sheet;
    private Row row;
    private InputStreamReader isr;
    private BufferedReader br;
    private Iterator<Row> rowIterator;
    private String line;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMdd");
    private InputStream fileDetect;
    private InputStream content;
    private String fileType;
    private String vet[] = null;
    private String[] array = null;
    private Patent patent;

    @Override
    public boolean initWithStream(InputStream is) {
        try {

            /*Clonando a inputstream para manipulá-la, são geradas outras 2 inputstream
             A primeira (fileDetect) é reponsável pela detecção do formato do arquivo
             A segunda (content) é o conteúdo em si da inputstream
             */
            cloneInputStream(is);
            isr = new InputStreamReader(fileDetect);
            br = new BufferedReader(isr);
            
            //Detectando o tipo de arquivo, a primeira linha do CSV sempre começa com "Title" ...
            if (!br.readLine().matches("\"Title.*")) {
                fileType = "xls";
                xlsManipulator();
            } else {
                fileType = "csv";
                next();
            }
            
        } catch (IOException ex) {
            //Se entrar aqui é porque o arquivo não está no padrão
            Logger.getLogger(ESPACENETPatentImporter.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }

    private void cloneInputStream(InputStream input) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // Fake code simulating the copy
            // You can generally do better with nio if you need...
            // And please, unlike me, do something about the Exceptions :D
            byte[] buffer = new byte[1024];
            int len;
            while ((len = input.read(buffer)) > -1) {
                baos.write(buffer, 0, len);
            }
            baos.flush();

            // Open new InputStreams using the recorded bytes
            // Can be repeated as many times as you wish
            fileDetect = new ByteArrayInputStream(baos.toByteArray());
            content = new ByteArrayInputStream(baos.toByteArray());

        } catch (IOException ex) {
            Logger.getLogger(ESPACENETPatentImporter.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public boolean hasNext() {
        if (fileType.equalsIgnoreCase("xls")) {
            if (rowIterator.hasNext()) {
                return true;
            }
        } else {
            if (line == null) {
                return false;
            }
            if (line.trim().length() <= 0) {
                return false;
            }
            return true;
        }

        return false;
    }

    @Override
    public Patent next() {
        patent = new Patent();
        if (fileType.equalsIgnoreCase("xls")) {
            parseLineXLS();
        } else {
            if (line != null) {
                parseLineCSV();
            }
            nextLine();
        }
        return patent;
    }

    @Override
    public void remove() {
    }

    private void xlsManipulator() {
        try {
            wb = new HSSFWorkbook(content);
            sheet = wb.getSheetAt(0);

            rowIterator = sheet.iterator();

            //Pulando primeiras linhas
            rowIterator.next(); //Logotipo
            rowIterator.next();// Numero de resultados encontrados na busca
            rowIterator.next(); // Titulo da pesquisa
            rowIterator.next(); // Quantidade de Publicações exibidas
            rowIterator.next(); // Nome das Colunas (Titulo, Publicação, Autor ...)
        } catch (IOException ex) {
            Logger.getLogger(ESPACENETPatentImporter.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /*
     * Metodo para clonar uma inputStream retirado do link abaixo
     * http://stackoverflow.com/questions/5923817/how-to-clone-an-inputstream
     */
    private void parseLineXLS() {

        String contentString;

        patent.setLanguage(lang);

        row = rowIterator.next(); //Percorrendo cada linha (patente)
        // Para cada linha (patente), pega cada atributo (Titulo, Publicação, Autor ...)
        Iterator<Cell> cellIterator = row.cellIterator();
        while (cellIterator.hasNext()) {

            Cell cell = cellIterator.next(); // Pegando cada coluna (atributo)

            switch (cell.getCellType()) {

                case Cell.CELL_TYPE_STRING:
                    contentString = cell.getStringCellValue().replaceAll("[\u2002]", " "); // Isso é para substituir o caracter especial por espaço em codificaçao UTF8
                    fillPatentXLS(cell.getColumnIndex(), contentString.replaceAll("[\u00e2][\u20ac][\u201a]", " ")); // // Isso é para substituir o caracter especial por espaço em codificaçao CP1252
                    break;

                default:
                    break;
            }

        }
    }

    private void fillPatentXLS(int columnIndex, String contentString) {
        Country nulCountry = new Country();
        nulCountry.setName("");
        nulCountry.setAcronym("");
        nulCountry.setStates(null);
        String aux;
        StringTokenizer st;
        switch (columnIndex) {
            case 0:
                patent.setTitleSelect(contentString);
                break;
            case 1:
                patent.setPublicationNumber(contentString);
                break;
            case 2:
                try {
                    patent.setPublicationDate(sdf.parse(contentString));
                } catch (ParseException ex) {
                    Logger.getLogger(ESPACENETPatentImporter.class.getName()).log(Level.SEVERE, null, ex);
                }
                break;
            case 3:
                List<Inventor> inventors = patent.getInventors();
                st = new StringTokenizer(contentString, "\n");
                while (st.hasMoreTokens()) {
                    StringTokenizer st2 = new StringTokenizer(st.nextToken());
                    Inventor inventor = new Inventor();
                    String name = new String();
                    String country = new String();
                    while (st2.hasMoreTokens()) {
                        aux = st2.nextToken();
                        if (aux.matches("\\[.+\\]+")) {
                            aux = aux.replace("[", "");
                            aux = aux.replace("]", "");
                            country = aux.trim();
                        } else {
                            name = name.concat(aux + " ");
                        }
                        inventor.setName(name);
                        inventor.setCountry(countryRepository.getCountryByAcronym(country));
                        if (inventor.getCountry() == null) {
                            inventor.setCountry(nulCountry);
                        }
                    }
                    if (!name.trim().isEmpty()) {
                        History h = new History();
                        h.setName(inventor.getName());
                        h.setCountry(inventor.getCountry());
                        inventor.setHistory(h);
                        inventors.add(inventor);
                    }
                }
                patent.setInventors(inventors);
                break;
            case 4:
                List<Applicant> applicants = patent.getApplicants();
                st = new StringTokenizer(contentString, "\n");
                while (st.hasMoreTokens()) {
                    StringTokenizer st2 = new StringTokenizer(st.nextToken());
                    Applicant applicant = new Applicant();
                    String name = new String();
                    String country = new String();
                    while (st2.hasMoreTokens()) {
                        aux = st2.nextToken();
                        if (aux.matches("\\[.+\\]+")) {
                            aux = aux.replace("[", "");
                            aux = aux.replace("]", "");
                            country = aux.trim();
                        } else {
                            name = name.concat(aux + " ");
                        }
                        applicant.setName(name);
                        applicant.setCountry(countryRepository.getCountryByAcronym(country));
                        if (applicant.getCountry() == null) {
                            applicant.setCountry(nulCountry);
                        }
                    }
                    if (!name.trim().isEmpty()) {
                        History h = new History();
                        h.setName(applicant.getName());
                        h.setCountry(applicant.getCountry());
                        applicant.setHistory(h);
                        applicants.add(applicant);
                    }
                }
                patent.setApplicants(applicants);
                break;
            case 5:
                List<Classification> classifications = patent.getClassifications();
                st = new StringTokenizer(contentString, "\n");
                while (st.hasMoreTokens()) {
                    Classification classification = new Classification();
                    classification.setType(ClassificationType.IC);
                    classification.setValue(st.nextToken());
                    classifications.add(classification);
                }
                try {
                    patent.setMainClassification(classifications.get(0));
                } catch (Exception ex) {
                }
                patent.setClassifications(classifications);
                break;
            case 6:
                //"Cooperative Patent Classification: ";
                List<Classification> cpcClassifications = patent.getCpcClassifications();
                st = new StringTokenizer(contentString, "\n");
                while (st.hasMoreTokens()) {
                    Classification cpcClassification = new Classification();
                    cpcClassification.setType(ClassificationType.IC);
                    cpcClassification.setValue(st.nextToken());
                    cpcClassifications.add(cpcClassification);
                }
                try {
                    patent.setMainCPCClassification(cpcClassifications.get(0));
                } catch (Exception ex) {
                }
                patent.setCpcClassifications(cpcClassifications);
                break;
            case 7:
                patent.setApplicationNumber(contentString);
                break;
            case 8:
                try {
                    patent.setApplicationDate(sdf2.parse(contentString));
                } catch (ParseException ex) {
                    Logger.getLogger(ESPACENETPatentImporter.class.getName()).log(Level.SEVERE, null, ex);
                }
                break;
            case 9:
                List<Priority> priorities = patent.getPriorities();
                st = new StringTokenizer(contentString, "\n");
                while (st.hasMoreTokens()) {
                    aux = st.nextToken();
                    Priority priority = new Priority();
                    priority.setCountry(countryRepository.getCountryByAcronym(aux.substring(0, 2)));
                    StringTokenizer st2 = new StringTokenizer(aux);
                    aux = st2.nextToken();
                    priority.setValue(aux.substring(2));
                    aux = st2.nextToken();
                    try {
                        priority.setDate(sdf2.parse(aux));
                    } catch (ParseException ex) {
                        Logger.getLogger(ESPACENETPatentImporter.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    priorities.add(priority);
                }
                patent.setPriorities(priorities);
                //"Priority number(s): ";
                //       pat.setPriorities(null);
                break;
        }

    }

    private void nextLine() {
        try {
            line = br.readLine();
        } catch (IOException e) {
            line = null;
            //e.printStackTrace();
        }
    }

    private void fillPatentCSV() {
        patent.setLanguage(lang);
        vet = line.split("\",");
        Country nulCountry = new Country();
        nulCountry.setName("");
        nulCountry.setAcronym("");
        nulCountry.setStates(null);

        List<Inventor> inventors = patent.getInventors();
        List<Applicant> applicants = patent.getApplicants();
        List<Classification> classifications = patent.getClassifications();
        List<Classification> cpcClassifications = patent.getCpcClassifications();
        List<Priority> priorities = patent.getPriorities();
        for (int i = 0; i < vet.length; i++) {
            array = vet[i].split(";");
            for (int j = 0; j < array.length; j++) {

                String contentString = array[j];
                contentString = contentString.replaceAll("\"", "");
                contentString = contentString.trim();

                switch (i) {
                    case 0:
                        patent.setTitleSelect(contentString);
                        break;
                    case 1:
                        patent.setPublicationNumber(contentString);
                        break;
                    case 2:
                        try {
                            patent.setPublicationDate(sdf.parse(contentString));
                        } catch (ParseException ex) {
                            Logger.getLogger(ESPACENETPatentImporter.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        break;
                    case 3:
                        if (!contentString.replaceAll("\\[.*]", "").trim().isEmpty()) {
                            Inventor inventor = new Inventor();
                            History history = new History();
                            inventor.setName(contentString.replaceAll("\\[.*]", "").trim());
                            try {
                                inventor.setCountry(countryRepository.getCountryByAcronym(contentString.substring(contentString.indexOf("[") + 1, contentString.indexOf("]"))));
                            } catch (IndexOutOfBoundsException ex) {
                            }
                            if (inventor.getCountry() == null) {
                                inventor.setCountry(nulCountry);
                            }
                            history.setName(inventor.getName());
                            history.setCountry(inventor.getCountry());
                            inventor.setHistory(history);
                            inventors.add(inventor);
                        }
                        break;
                    case 4:
                        if (!contentString.replaceAll("\\[.*]", "").trim().isEmpty()) {
                            Applicant applicant = new Applicant();
                            History history = new History();
                            applicant.setName(contentString.replaceAll("\\[.*]", "").trim());
                            try {
                                applicant.setCountry(countryRepository.getCountryByAcronym(contentString.substring(contentString.indexOf("[") + 1, contentString.indexOf("]"))));
                            } catch (IndexOutOfBoundsException ex) {
                            }
                            if (applicant.getCountry() == null) {
                                applicant.setCountry(nulCountry);
                            }
                            history.setName(applicant.getName());
                            history.setCountry(applicant.getCountry());
                            applicant.setHistory(history);
                            applicants.add(applicant);
                        }
                        break;
                    case 5:
                        Classification classification = new Classification();
                        classification.setValue(contentString);
                        classification.setType(ClassificationType.IC);
                        classifications.add(classification);
                        break;
                    case 6:
                        //"Cooperative Patent Classification: ";
                        Classification cpcClassification = new Classification();
                        cpcClassification.setValue(contentString);
                        cpcClassification.setType(ClassificationType.IC);
                        cpcClassifications.add(cpcClassification);
                        break;
                    case 7:
                        patent.setApplicationNumber(contentString);
                        break;
                    case 8:
                        try {
                            patent.setApplicationDate(sdf2.parse(contentString));
                        } catch (ParseException ex) {
                            Logger.getLogger(ESPACENETPatentImporter.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        break;
                    case 9:
                        Priority priority = new Priority();
                        try {
                            priority.setCountry(countryRepository.getCountryByAcronym(contentString.substring(0, 2)));
                        } catch (IndexOutOfBoundsException ex) {
                        }
                        try {
                            priority.setValue(contentString.substring(2, contentString.indexOf(" ")).trim());
                        } catch (IndexOutOfBoundsException ex) {
                        }
                        try {

                            priority.setDate(sdf2.parse(contentString.substring(contentString.indexOf(" ")).trim()));
                        } catch (ParseException ex) {
                            Logger.getLogger(ESPACENETPatentImporter.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (IndexOutOfBoundsException ex) {
                        }
                        priorities.add(priority);
                        break;
                }
            }
        }
        patent.setApplicants(applicants);
        patent.setClassifications(classifications);
        patent.setCpcClassifications(cpcClassifications);
        try {
            patent.setMainClassification(classifications.get(0));
            patent.setMainCPCClassification(cpcClassifications.get(0));
        } catch (Exception ex) {
        }
        patent.setPriorities(priorities);
        patent.setInventors(inventors);

    }

    private void parseLineCSV() {
        fillPatentCSV();
    }

    @Override
    public String provider() {
        return "ESPACENET";
    }
}
