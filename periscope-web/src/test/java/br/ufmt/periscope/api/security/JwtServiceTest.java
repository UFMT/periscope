package br.ufmt.periscope.api.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        jwtService.setSecretForTests("unit-test-secret-key-32bytes!!".getBytes(StandardCharsets.UTF_8));
        jwtService.setExpirationSecondsForTests(3600);
    }

    @Test
    void issuesAndValidatesTokenWithUniqueJti() {
        String token1 = jwtService.issueToken("admin", "ADMIN");
        String token2 = jwtService.issueToken("admin", "ADMIN");
        assertThat(token1.split("\\.")).hasSize(3);

        JwtService.JwtClaims claims1 = jwtService.parseAndValidate(token1);
        JwtService.JwtClaims claims2 = jwtService.parseAndValidate(token2);
        assertThat(claims1.subject()).isEqualTo("admin");
        assertThat(claims1.userLevel()).isEqualTo("ADMIN");
        assertThat(claims1.exp()).isGreaterThan(claims1.iat());
        assertThat(claims1.jti()).isNotBlank();
        assertThat(claims2.jti()).isNotBlank().isNotEqualTo(claims1.jti());
    }

    @Test
    void rejectsTamperedSignature() {
        String token = jwtService.issueToken("admin", "ADMIN");
        String tampered = token.substring(0, token.length() - 2) + "ab";
        assertThatThrownBy(() -> jwtService.parseAndValidate(tampered))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("signature");
    }

    @Test
    void rejectsNonHs256Algorithm() {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"admin\",\"exp\":9999999999,\"iat\":1}".getBytes(StandardCharsets.UTF_8));
        String forged = header + "." + payload + ".sig";
        assertThatThrownBy(() -> jwtService.parseAndValidate(forged))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("algorithm");
    }

    @Test
    void rejectsExpiredToken() {
        jwtService.setExpirationSecondsForTests(-10);
        String token = jwtService.issueToken("admin", "USER");
        assertThatThrownBy(() -> jwtService.parseAndValidate(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void rejectsMalformedToken() {
        assertThatThrownBy(() -> jwtService.parseAndValidate("not-a-jwt"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> jwtService.parseAndValidate(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requiresStrongSecretInProductionMode() {
        JwtService prodService = new JwtService();
        assertThatThrownBy(() -> prodService.init(Map.of("PERISCOPE_PRODUCTION", "true")::get))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PERISCOPE_JWT_SECRET");

        assertThatThrownBy(() -> prodService.init(Map.of(
                "PERISCOPE_PRODUCTION", "true",
                "PERISCOPE_JWT_SECRET", "too-short")::get))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");

        prodService.init(Map.of(
                "PERISCOPE_PRODUCTION", "true",
                "PERISCOPE_JWT_SECRET", "this-is-a-strong-production-secret-key-with-32-plus-bytes!")::get);
        String tok = prodService.issueToken("admin", "ADMIN");
        assertThat(prodService.parseAndValidate(tok).subject()).isEqualTo("admin");
    }
}
