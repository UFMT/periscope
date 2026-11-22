package br.ufmt.periscope.api.security;

import br.ufmt.periscope.model.User;
import br.ufmt.periscope.security.UserAuthenticator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AuthService {

    @Inject
    private UserAuthenticator userAuthenticator;

    @Inject
    private JwtService jwtService;

    @Inject
    private TokenRevocationStore revocationStore;

    public AuthResult authenticate(String username, String password) {
        User user = userAuthenticator.authenticate(username, password);
        if (user == null) {
            return null;
        }
        String level = user.getUserLevel() != null ? user.getUserLevel().name() : "USER";
        String token = jwtService.issueToken(user.getUsername(), level);
        return new AuthResult(token, user);
    }

    public JwtService.JwtClaims validate(String token) {
        JwtService.JwtClaims claims = jwtService.parseAndValidate(token);
        if (revocationStore != null && claims.jti() != null && revocationStore.isRevoked(claims.jti())) {
            throw new IllegalArgumentException("Token revoked");
        }
        return claims;
    }

    public void revoke(JwtService.JwtClaims claims) {
        if (revocationStore != null && claims != null && claims.jti() != null) {
            revocationStore.revoke(claims.jti(), claims.exp());
        }
    }

    public AuthResult refresh(User user, JwtService.JwtClaims currentClaims) {
        revoke(currentClaims);
        String level = user.getUserLevel() != null ? user.getUserLevel().name() : "USER";
        String token = jwtService.issueToken(user.getUsername(), level);
        return new AuthResult(token, user);
    }

    public long getExpirationSeconds() {
        return jwtService != null ? jwtService.getExpirationSeconds() : 8 * 3600L;
    }

    public record AuthResult(String token, User user) {
    }
}
