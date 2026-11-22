package br.ufmt.periscope.api.dto;

import java.util.List;

public record PatentUpdateRequest(
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
        Boolean completed
) {
    public PatentUpdateRequest(String title, String abstractText, Boolean blacklisted, Boolean completed) {
        this(title, abstractText, null, null, null, null, null, null, null, null, null, null, null,
                blacklisted, completed);
    }
}
