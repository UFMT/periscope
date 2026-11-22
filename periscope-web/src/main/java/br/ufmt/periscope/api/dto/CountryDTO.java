package br.ufmt.periscope.api.dto;

import java.util.List;

public record CountryDTO(String acronym, String name, List<StateDTO> states) {
}
