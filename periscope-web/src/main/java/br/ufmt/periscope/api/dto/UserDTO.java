package br.ufmt.periscope.api.dto;

import br.ufmt.periscope.model.User;
import br.ufmt.periscope.model.UserLevel;

public record UserDTO(
        String id,
        String username,
        String firstname,
        String lastname,
        String email,
        String userLevel,
        boolean mustChangePassword
) {
    public static UserDTO from(User user) {
        if (user == null) {
            return null;
        }
        UserLevel level = user.getUserLevel();
        return new UserDTO(
                user.getId() != null ? user.getId().toString() : null,
                user.getUsername(),
                user.getFirstname(),
                user.getLastname(),
                user.getEmail(),
                level != null ? level.name() : null,
                Boolean.TRUE.equals(user.getMustChangePassword())
        );
    }
}
