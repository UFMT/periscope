package br.ufmt.periscope.importer.decorator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import br.ufmt.periscope.enumerated.ClassificationType;
import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.model.ApplicantType;
import br.ufmt.periscope.model.Classification;
import br.ufmt.periscope.model.Inventor;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Priority;
import br.ufmt.periscope.repository.ApplicantTypeRepository;

@ApplicationScoped
@Named
public class PatentValidator {

    private @Inject
    ApplicantTypeRepository typeRepository;

    public void validate(Patent patent) {
        if (patent == null) {
            return;
        }

        validateClassifications(patent);
        clearStatesFromCountries(patent);
        fillInventors(patent);
        saveApplicantTypes(patent);

        patent.setCompleted(false);
        setAsComplete(patent);
    }

    private void clearStatesFromCountries(Patent patent) {
        if (patent.getApplicants() != null) {
            for (Applicant pa : patent.getApplicants()) {
                if (pa != null && pa.getCountry() != null) {
                    pa.getCountry().setStates(null);
                }
            }
        }
        if (patent.getInventors() != null) {
            for (Inventor inv : patent.getInventors()) {
                if (inv != null && inv.getCountry() != null) {
                    inv.getCountry().setStates(null);
                }
            }
        }
        if (patent.getPriorities() != null) {
            for (Priority priority : patent.getPriorities()) {
                if (priority != null && priority.getCountry() != null) {
                    priority.getCountry().setStates(null);
                }
            }
        }
    }

    private void saveApplicantTypes(Patent patent) {
        if (patent.getApplicants() == null || typeRepository == null) {
            return;
        }
        Set<String> types = new HashSet<String>();
        for (Applicant pa : patent.getApplicants()) {
            if (pa == null || pa.getType() == null || pa.getType().getName() == null
                    || pa.getType().getName().isBlank()) {
                continue;
            }
            types.add(pa.getType().getName());
        }
        for (String type : types) {
            typeRepository.createIfNotExists(new ApplicantType(type));
        }
    }

    private void setAsComplete(Patent patent) {
        if (patent.getApplicationNumber() != null
                && patent.getApplicationDate() != null
                && patent.getApplicants() != null && !patent.getApplicants().isEmpty()
                && patent.getTitleSelect() != null
                && patent.getMainClassification() != null
                && patent.getInventors() != null && !patent.getInventors().isEmpty()) {
            patent.setCompleted(true);
        }
    }

    private void fillInventors(Patent patent) {
        List<Applicant> apps = patent.getApplicants();
        if (apps == null) {
            return;
        }
        List<Inventor> inventors = patent.getInventors();
        if (inventors == null) {
            inventors = new ArrayList<>();
            patent.setInventors(inventors);
        }
        if (patent.getLanguage() != null && patent.getLanguage().equalsIgnoreCase("EN")
                && inventors.isEmpty()) {
            for (Applicant pa : apps) {
                if (pa == null) {
                    continue;
                }
                Inventor inventor = new Inventor();
                inventor.setName(pa.getName());
                if (pa.getCountry() != null) {
                    inventor.setCountry(pa.getCountry());
                } else {
                    inventor.setCountry(null);
                }
                inventors.add(inventor);
            }
            patent.setInventors(inventors);
        }
        if (apps.size() > 1) {
            patent.setShared(true);
        }
    }

    private void validateClassifications(Patent patent) {
        if (patent.getClassifications() == null) {
            return;
        }
        Classification icai = null, ic = null, icci = null;
        for (Classification classification : patent.getClassifications()) {
            if (classification == null) {
                continue;
            }
            if (classification.getType() == ClassificationType.ICAI && icai == null) {
                icai = classification;
            }
            if (classification.getType() == ClassificationType.IC && ic == null) {
                ic = classification;
            }
            if (classification.getType() == ClassificationType.ICCI && icci == null) {
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
