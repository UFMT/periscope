package br.ufmt.periscope.api.dto;

import br.ufmt.periscope.model.User;

public record UserSummaryDTO(
        String id,
        String username,
        String firstname,
        String lastname
) {
    public static UserSummaryDTO from(User user) {
        if (user == null) {
            return null;
        }
        return new UserSummaryDTO(
                user.getId() != null ? user.getId().toString() : null,
                user.getUsername(),
                user.getFirstname(),
                user.getLastname()
        );
    }
}
