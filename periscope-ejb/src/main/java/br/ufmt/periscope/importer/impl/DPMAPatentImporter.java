package br.ufmt.periscope.importer.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import br.ufmt.periscope.enumerated.ClassificationType;
import br.ufmt.periscope.importer.PatentImporter;
import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.model.Classification;
import br.ufmt.periscope.model.Inventor;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.repository.CountryRepository;

@Named
public class DPMAPatentImporter implements PatentImporter {

    private @Inject
    CountryRepository countryRepository;
    private BufferedReader bur = null;
    private String line = null;
    private Patent patent = null;
    private String lang = "EN";
    private SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
    private SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMdd");
    private String vet[] = null;
    private String[] array = null;

    @Override
    public boolean initWithStream(InputStream is) {
        InputStreamReader isr = new InputStreamReader(is);
        bur = new BufferedReader(isr);
        //Passa o cabeçalho
        nextLine();
        //Inicia na primeira linha dos dados
        nextLine();
        if (testLine(line)) {
            patent = new Patent();
            return true;
        } else {
            return false;
        }

    }

    @Override
    public boolean hasNext() {
        if (line == null) {
            return false;
        }
        if (line.trim().length() <= 0) {
            return false;
        }
        return true;
    }

    @Override
    public Patent next() {
        try {
            parseLine();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        nextLine();
        return patent;

    }

    private void parseLine() throws ParseException {
        patent = new Patent();
        vet = line.split("; ", -2);

        patent.setLanguage(lang);
        if (vet[6].length() > 5) {
            patent.setTitleSelect(vet[6].substring(5));
        } else {
            patent = null;
            return;
        }
        patent.setPublicationNumber(vet[0] + " " + sdf2.format(sdf.parse(vet[2])));
        patent.setPublicationCountry(countryRepository.getCountryByAcronym(patent.getPublicationNumber().substring(0, 2).toUpperCase()));
        patent.setPublicationDate(sdf.parse(vet[2]));

        //patent.setApplicationNumber(vet[0] + " " + 	sdf2.format(sdf.parse(vet[2])));
        patent.setApplicationDate(sdf.parse(vet[1]));
        patent.setApplicationCountry(countryRepository.getCountryByAcronym(patent.getPublicationNumber().substring(0, 2).toUpperCase()));


        //Carrega as classificações
        readClassifications();
        readApplicants();
        readInventors();
    }

    private Boolean testLine(String test) {

        String vetor[] = test.split(";", -2);
        if (vetor.length >= 6) {
            return true;
        } else {
            return false;
        }

    }

    private void readInventors() {
        List<Inventor> listInventors = patent.getInventors();
        array = vet[4].split(", ", -2);
        Inventor inventor = new Inventor();
        if (array[0].trim().length() > 0) {
            inventor.setName(array[0].trim());
            if (1 < array.length) {
                inventor.setCountry(countryRepository.getCountryByAcronym(array[1].substring(0, 2)));
            }
            listInventors.add(inventor);
            for (int i = 1; i < array.length; i++) {
                if (array[i].trim().length() > 3) {

                    inventor = new Inventor();
                    inventor.setName(array[i].substring(2).trim());
                    if (i + 1 < array.length) {
                        inventor.setCountry(countryRepository.getCountryByAcronym(array[i + 1].substring(0, 2)));
                    }
                    listInventors.add(inventor);
                }
            }
        }
        patent.setInventors(listInventors);
    }

    private void readApplicants() {

        List<Applicant> listPa = patent.getApplicants();
        array = vet[5].split(", ", -2);
        Applicant applicant = new Applicant();
        if (array[0].trim().length() > 0) {
            applicant.setName(array[0].trim());
            if (1 < array.length) {
                applicant.setCountry(countryRepository.getCountryByAcronym(array[1].substring(0, 2)));
            }
            listPa.add(applicant);
            for (int i = 1; i < array.length; i++) {
                if (array[i].trim().length() > 3) {
                    applicant = new Applicant();
                    applicant.setName(array[i].substring(2).trim());
                    if (i + 1 < array.length) {
                        applicant.setCountry(countryRepository.getCountryByAcronym(array[i + 1].substring(0, 2)));
                    }
                    if (applicant.getCountry() == null) {
                        applicant.setCountry(null);
                    }
                    listPa.add(applicant);
                }
            }
        }
        patent.setApplicants(listPa);

    }

    private void readClassifications() {
        List<Classification> icais = patent.getClassifications();
        String string = null;
        if (vet[3].trim().length() > 0) {
            string = vet[3].replace(" ", "");
            array = string.split("/", -2);
            String tudo = "";
            try {
                tudo = string.replace(" ", "");
            } catch (NumberFormatException e) {
                if (array != null) {
                    tudo = array[0].substring(0, 4);
                }
            }
            Classification icai = new Classification(tudo, ClassificationType.ICAI);
            if (patent.getMainClassification() == null) {
                patent.setMainClassification(icai);
            }
            icais.add(icai);
        }
        patent.setClassifications(icais);
    }

    private void nextLine() {
        try {
            line = bur.readLine();
        } catch (IOException e) {
            line = null;
            e.printStackTrace();
        }
    }

    @Override
    public void remove() {
    }

    @Override
    public String provider() {
        return "DPMA";
    }
}
