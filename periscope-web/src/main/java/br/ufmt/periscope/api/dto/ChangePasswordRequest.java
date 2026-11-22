package br.ufmt.periscope.api.dto;

public record ChangePasswordRequest(String currentPassword, String newPassword) {
}
