package br.ufmt.periscope.api.dto;

import java.time.Instant;

public record PriorityDTO(String value, String countryAcronym, Instant date) {
}
