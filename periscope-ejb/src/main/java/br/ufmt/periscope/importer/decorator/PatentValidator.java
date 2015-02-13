package br.ufmt.periscope.importer.decorator;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import br.ufmt.periscope.enumerated.ClassificationType;
import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.model.ApplicantType;
import br.ufmt.periscope.model.Classification;
import br.ufmt.periscope.model.Inventor;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Priority;
import br.ufmt.periscope.repository.ApplicantTypeRepository;

@Named
public class PatentValidator {

    private Patent patent;
    private @Inject
    ApplicantTypeRepository typeRepository;

    public void validate(Patent patent) {
        this.patent = patent;

        validateClassifications();
        clearStatesFromCountries();
        fillInventors();
        saveApplicantTypes();

        patent.setCompleted(false);

        //validatePublicationAndApplicationNumbers();		
        setAsComplete();

    }

    private void clearStatesFromCountries() {
        for (Applicant pa : patent.getApplicants()) {
            if (pa.getCountry() != null) {
                pa.getCountry().setStates(null);
            }
        }
        for (Inventor inv : patent.getInventors()) {
            if (inv.getCountry() != null) {
                inv.getCountry().setStates(null);
            }
        }
        for (Priority priority : patent.getPriorities()) {
            if (priority.getCountry() != null) {
                priority.getCountry().setStates(null);
            }
        }
    }

    private void saveApplicantTypes() {
        Set<String> types = new HashSet<String>();
        for (Applicant pa : patent.getApplicants()) {

            if (pa.getType() == null) {
                continue;
            }

            types.add(pa.getType().getName());
        }
        for (String type : types) {
            typeRepository.createIfNotExists(new ApplicantType(type));
        }
    }

    private void setAsComplete() {

        if (patent.getApplicationNumber() != null) {
            if (patent.getApplicationDate() != null) {
                if (patent.getApplicants().size() > 0) {
                    if (patent.getTitleSelect() != null) {
                        if (patent.getMainClassification() != null) {
                            if (patent.getInventors().size() > 0) {
                                patent.setCompleted(true);
                            }
                        }
                    }
                }
            }
        }
    }

    private void validatePublicationAndApplicationNumbers() {
        if (patent.getApplicationNumber() != null && patent.getApplicationNumber().trim().length() > 0) {
            String apn = patent.getApplicationNumber();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            int tam = apn.length();
            try {
                patent.setApplicationDate(sdf.parse(apn.substring(tam - 8)));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        if (patent.getPublicationNumber() != null && patent.getPublicationNumber().trim().length() > 0) {
            String pn = patent.getPublicationNumber();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            int tam = pn.length();
            try {
                patent.setPublicationDate(sdf.parse(pn.substring(tam - 8)));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

    }

    private void fillInventors() {
        if (patent.getLanguage() != null && patent.getLanguage().equalsIgnoreCase("EN")
                && patent.getInventors().size() <= 0) {

            List<Applicant> apps = patent.getApplicants();
            Inventor inventor = null;
            List<Inventor> inventors = patent.getInventors();
            for (Applicant pa : apps) {

                inventor = new Inventor();
                inventor.setName(pa.getName());
                if (pa.getCountry() != null) {
                    inventor.setCountry(pa.getCountry());
                } else {
                    inventor.setCountry(null);
                };

                inventors.add(inventor);
            }
            patent.setInventors(inventors);
        }
        if (patent.getApplicants().size() > 1) {
            patent.setShared(true);
        }

    }

    private void validateClassifications() {
        Classification icai = null, ic = null, icci = null;
        for (Classification classification : patent.getClassifications()) {
            if (classification.getType() == ClassificationType.ICAI && icai == null) {
                icai = classification;
            }
            if (classification.getType() == ClassificationType.IC && ic == null) {
                ic = classification;
            }
            if (classification.getType() == ClassificationType.ICAI && icci == null) {
                icci = classification;
            }
        }
        if (icai != null) {
            patent.setMainClassification(icai);
        } else if (ic != null) {
            patent.setMainClassification(ic);
        } else if (icci != null) {
            patent.setMainClassification(icci);
        }

    }
}
