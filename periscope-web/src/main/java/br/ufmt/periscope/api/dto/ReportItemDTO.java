package br.ufmt.periscope.api.dto;

public record ReportItemDTO(String key, Number value, String description) {
    public ReportItemDTO(String key, Number value) {
        this(key, value, null);
    }
}
