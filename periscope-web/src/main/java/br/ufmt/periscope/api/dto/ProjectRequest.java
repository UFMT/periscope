package br.ufmt.periscope.api.dto;

import java.util.List;

public record ProjectRequest(
        String title,
        String description,
        Boolean isPublic,
        List<String> observerIds
) {
    public ProjectRequest(String title, String description, Boolean isPublic) {
        this(title, description, isPublic, null);
    }
}
