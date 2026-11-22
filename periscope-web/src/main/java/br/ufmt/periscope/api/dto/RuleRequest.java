package br.ufmt.periscope.api.dto;

import java.util.Set;

public record RuleRequest(
        String name,
        String acronym,
        String type,
        Set<String> substitutions,
        String countryAcronym,
        String stateAcronym,
        String nature
) {
    public RuleRequest(
            String name,
            String acronym,
            String type,
            Set<String> substitutions,
            String countryAcronym,
            String nature) {
        this(name, acronym, type, substitutions, countryAcronym, null, nature);
    }
}
