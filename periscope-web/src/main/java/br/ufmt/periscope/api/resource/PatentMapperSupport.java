package br.ufmt.periscope.api.resource;

import br.ufmt.periscope.api.ApiException;
import br.ufmt.periscope.api.dto.PriorityInputDTO;
import br.ufmt.periscope.enumerated.ClassificationType;
import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.model.Classification;
import br.ufmt.periscope.model.Country;
import br.ufmt.periscope.model.Inventor;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Priority;
import br.ufmt.periscope.repository.CountryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared mapping and date-parsing utilities for patent REST endpoints.
 */
@ApplicationScoped
public class PatentMapperSupport {

    @Inject
    private CountryRepository countryRepository;

    public PatentMapperSupport() {
    }

    public PatentMapperSupport(CountryRepository countryRepository) {
        this.countryRepository = countryRepository;
    }

    public void applyFields(
            Patent patent,
            String title,
            String abstractText,
            String publicationNumber,
            String publicationDate,
            String applicationNumber,
            String applicationDate,
            String applicationCountry,
            String language,
            String mainClassification,
            List<String> classifications,
            List<String> applicants,
            List<String> inventors,
            List<PriorityInputDTO> priorities,
            Boolean blacklisted,
            Boolean completed) {

        if (title != null) {
            patent.setTitleSelect(title.trim());
        }
        if (abstractText != null) {
            patent.setAbstractSelect(abstractText);
        }
        if (publicationNumber != null) {
            patent.setPublicationNumber(blankToNull(publicationNumber));
        }
        if (publicationDate != null) {
            patent.setPublicationDate(publicationDate.isBlank() ? null : parseDate(publicationDate));
        }
        if (applicationNumber != null) {
            patent.setApplicationNumber(blankToNull(applicationNumber));
        }
        if (applicationDate != null) {
            patent.setApplicationDate(applicationDate.isBlank() ? null : parseDate(applicationDate));
        }
        if (applicationCountry != null) {
            patent.setApplicationCountry(resolveCountry(applicationCountry));
        }
        if (language != null) {
            patent.setLanguage(blankToNull(language));
        }
        if (mainClassification != null || classifications != null) {
            applyClassifications(patent, mainClassification, classifications);
        }
        if (applicants != null) {
            patent.setApplicants(mergeApplicants(patent.getApplicants(), applicants));
        }
        if (inventors != null) {
            patent.setInventors(mergeInventors(patent.getInventors(), inventors));
        }
        if (priorities != null) {
            patent.setPriorities(mapPriorities(priorities));
        }
        if (blacklisted != null) {
            patent.setBlacklisted(blacklisted);
        }
        if (completed != null) {
            patent.setCompleted(completed);
        }
    }

    private void applyClassifications(Patent patent, String mainClassification, List<String> classifications) {
        String main = blankToNull(mainClassification);
        if (main == null && patent.getMainClassification() != null && mainClassification == null) {
            main = patent.getMainClassification().getValue();
        }
        List<Classification> result = new ArrayList<>();
        if (main != null) {
            Classification mainCls = new Classification(main, ClassificationType.ICAI);
            patent.setMainClassification(mainCls);
            result.add(mainCls);
        } else if (mainClassification != null && mainClassification.isBlank()) {
            patent.setMainClassification(null);
        }

        List<String> extras = classifications;
        if (extras == null && patent.getClassifications() != null) {
            extras = patent.getClassifications().stream()
                    .filter(c -> c != null && c.getValue() != null)
                    .map(Classification::getValue)
                    .toList();
        }
        if (extras != null) {
            for (String raw : extras) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                String normalized = raw.trim().toUpperCase();
                if (main != null && normalized.equalsIgnoreCase(main)) {
                    continue;
                }
                result.add(new Classification(normalized, ClassificationType.IC));
            }
        }
        patent.setClassifications(result);
    }

    private List<Applicant> mergeApplicants(List<Applicant> existing, List<String> names) {
        Map<String, Applicant> byUpperName = new LinkedHashMap<>();
        if (existing != null) {
            for (Applicant a : existing) {
                if (a != null && a.getName() != null) {
                    byUpperName.putIfAbsent(a.getName().toUpperCase(), a);
                }
            }
        }
        List<Applicant> out = new ArrayList<>();
        for (String raw : names) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String upper = raw.trim().toUpperCase();
            Applicant prev = byUpperName.get(upper);
            out.add(prev != null ? prev : new Applicant(upper));
        }
        return out;
    }

    private List<Inventor> mergeInventors(List<Inventor> existing, List<String> names) {
        Map<String, Inventor> byUpperName = new LinkedHashMap<>();
        if (existing != null) {
            for (Inventor i : existing) {
                if (i != null && i.getName() != null) {
                    byUpperName.putIfAbsent(i.getName().toUpperCase(), i);
                }
            }
        }
        List<Inventor> out = new ArrayList<>();
        for (String raw : names) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String upper = raw.trim().toUpperCase();
            Inventor prev = byUpperName.get(upper);
            out.add(prev != null ? prev : new Inventor(upper));
        }
        return out;
    }

    private List<Priority> mapPriorities(List<PriorityInputDTO> inputs) {
        List<Priority> out = new ArrayList<>();
        for (PriorityInputDTO input : inputs) {
            if (input == null) {
                continue;
            }
            boolean hasValue = input.value() != null && !input.value().isBlank();
            boolean hasCountry = input.countryAcronym() != null && !input.countryAcronym().isBlank();
            boolean hasDate = input.date() != null && !input.date().isBlank();
            if (!hasValue && !hasCountry && !hasDate) {
                continue;
            }
            Priority p = new Priority();
            if (hasValue) {
                p.setValue(input.value().trim());
            }
            p.setCountry(resolveCountry(input.countryAcronym()));
            if (hasDate) {
                p.setDate(parseDate(input.date()));
            }
            out.add(p);
        }
        return out;
    }

    public Country resolveCountry(String acronym) {
        if (acronym == null || acronym.isBlank()) {
            return null;
        }
        String code = acronym.trim().toUpperCase();
        Country found = countryRepository != null ? countryRepository.getCountryByAcronym(code) : null;
        Country copy = new Country();
        if (found != null) {
            copy.setId(found.getId());
            copy.setAcronym(found.getAcronym());
            copy.setName(found.getName());
        } else {
            copy.setAcronym(code);
            copy.setName(code);
        }
        copy.setStates(null);
        return copy;
    }

    public static Date parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        try {
            return Date.from(Instant.parse(trimmed));
        } catch (DateTimeParseException ignored) {
            // Try ISO local date or offset date time
        }
        try {
            return Date.from(OffsetDateTime.parse(trimmed).toInstant());
        } catch (DateTimeParseException ignored) {
            // Try YYYY-MM-DD
        }
        try {
            LocalDate ld = LocalDate.parse(trimmed);
            return Date.from(ld.atStartOfDay(ZoneOffset.UTC).toInstant());
        } catch (DateTimeParseException ex) {
            throw ApiException.badRequest("Invalid date format (expected YYYY-MM-DD or ISO-8601): " + raw);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
