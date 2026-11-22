package br.ufmt.periscope.api.dto;

import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.model.Classification;
import br.ufmt.periscope.model.Country;
import br.ufmt.periscope.model.Inventor;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Priority;
import java.time.Instant;
import java.util.Date;
import java.util.List;

public record PatentDTO(
        String id,
        String title,
        String abstractText,
        String publicationNumber,
        Instant publicationDate,
        String applicationNumber,
        Instant applicationDate,
        String applicationCountry,
        String mainClassification,
        Boolean blacklisted,
        Boolean completed,
        String projectId,
        List<String> applicants,
        List<String> inventors,
        String presentationFileId,
        String patentInfoFileId,
        String language,
        List<String> classifications,
        List<PriorityDTO> priorities
) {
    /**
     * Maps a patent using an explicit project id — preferred for list endpoints
     * so Morphia never has to resolve {@code Patent.project} (@Reference).
     */
    public static PatentDTO from(Patent patent, String projectId) {
        if (patent == null) {
            return null;
        }
        Country appCountry = patent.getApplicationCountry();
        return new PatentDTO(
                patent.getId() != null ? patent.getId().toString() : null,
                patent.getTitleSelect(),
                patent.getAbstractSelect(),
                patent.getPublicationNumber(),
                toInstant(patent.getPublicationDate()),
                patent.getApplicationNumber(),
                toInstant(patent.getApplicationDate()),
                appCountry != null ? appCountry.getAcronym() : null,
                patent.getMainClassification() != null ? patent.getMainClassification().getValue() : null,
                patent.getBlacklisted(),
                patent.getCompleted(),
                projectId,
                mapApplicants(patent.getApplicants()),
                mapInventors(patent.getInventors()),
                patent.getPresentationFile() != null && patent.getPresentationFile().getId() != null
                        ? patent.getPresentationFile().getId().toString() : null,
                patent.getPatentInfo() != null && patent.getPatentInfo().getId() != null
                        ? patent.getPatentInfo().getId().toString() : null,
                patent.getLanguage(),
                mapClassifications(patent.getClassifications()),
                mapPriorities(patent.getPriorities())
        );
    }

    /**
     * Prefer {@link #from(Patent, String)} when the project id is already known
     * (list/import paths). This overload only reads {@code project} if a stub
     * with id was already set — it is not safe on a Morphia-lazy reference
     * while another cursor is open.
     */
    public static PatentDTO from(Patent patent) {
        if (patent == null) {
            return null;
        }
        String projectId = null;
        if (patent.getProject() != null && patent.getProject().getId() != null) {
            projectId = patent.getProject().getId().toString();
        }
        return from(patent, projectId);
    }

    private static List<String> mapApplicants(List<Applicant> applicants) {
        if (applicants == null) {
            return List.of();
        }
        return applicants.stream()
                .filter(a -> a != null && a.getName() != null)
                .map(Applicant::getName)
                .toList();
    }

    private static List<String> mapInventors(List<Inventor> inventors) {
        if (inventors == null) {
            return List.of();
        }
        return inventors.stream()
                .filter(i -> i != null && i.getName() != null)
                .map(Inventor::getName)
                .toList();
    }

    private static List<String> mapClassifications(List<Classification> classifications) {
        if (classifications == null) {
            return List.of();
        }
        return classifications.stream()
                .filter(c -> c != null && c.getValue() != null)
                .map(Classification::getValue)
                .toList();
    }

    private static List<PriorityDTO> mapPriorities(List<Priority> priorities) {
        if (priorities == null) {
            return List.of();
        }
        return priorities.stream()
                .filter(p -> p != null)
                .map(p -> new PriorityDTO(
                        p.getValue(),
                        p.getCountry() != null ? p.getCountry().getAcronym() : null,
                        toInstant(p.getDate())))
                .toList();
    }

    private static Instant toInstant(Date date) {
        return date == null ? null : date.toInstant();
    }
}
