package br.ufmt.periscope.api.dto;

import br.ufmt.periscope.model.Rule;
import br.ufmt.periscope.model.RuleType;
import java.util.Set;

public record RuleDTO(
        String id,
        String name,
        String acronym,
        String type,
        Set<String> substitutions,
        String countryAcronym,
        String stateAcronym,
        String nature,
        String projectId
) {
    public static RuleDTO from(Rule rule, String projectId) {
        if (rule == null) {
            return null;
        }
        RuleType type = rule.getType();
        return new RuleDTO(
                rule.getId() != null ? rule.getId().toString() : null,
                rule.getName(),
                rule.getAcronym(),
                type != null ? type.name() : null,
                rule.getSubstitutions(),
                rule.getCountry() != null ? rule.getCountry().getAcronym() : null,
                rule.getState() != null ? rule.getState().getAcronym() : null,
                rule.getNature() != null ? rule.getNature().getName() : null,
                projectId
        );
    }

    public static RuleDTO from(Rule rule) {
        if (rule == null) {
            return null;
        }
        String projectId = null;
        if (rule.getProject() != null && rule.getProject().getId() != null) {
            projectId = rule.getProject().getId().toString();
        }
        return from(rule, projectId);
    }
}
