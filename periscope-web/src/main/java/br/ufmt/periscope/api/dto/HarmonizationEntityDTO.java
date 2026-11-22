package br.ufmt.periscope.api.dto;

public record HarmonizationEntityDTO(
        String name,
        String acronym,
        String countryAcronym,
        String stateAcronym,
        String nature,
        Boolean harmonized,
        int documentCount
) {
}
