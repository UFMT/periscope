package br.ufmt.periscope.api.resource;

import br.ufmt.periscope.api.ApiException;
import br.ufmt.periscope.api.dto.ChangePasswordRequest;
import br.ufmt.periscope.api.dto.LoginRequest;
import br.ufmt.periscope.api.dto.LoginResponse;
import br.ufmt.periscope.api.dto.UserDTO;
import br.ufmt.periscope.api.security.ApiSecuritySupport;
import br.ufmt.periscope.api.security.AuthService;
import br.ufmt.periscope.api.security.JwtAuthFilter;
import br.ufmt.periscope.api.security.JwtService;
import br.ufmt.periscope.model.User;
import br.ufmt.periscope.model.UserLevel;
import br.ufmt.periscope.security.PasswordHasher;
import dev.morphia.Datastore;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthResourceTest {

    @Mock
    private AuthService authService;

    @Mock
    private ApiSecuritySupport securitySupport;

    @Mock
    private Datastore ds;

    @InjectMocks
    private AuthResource authResource;

    private User user;
    private SecurityContext secCtx;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(new ObjectId());
        user.setUsername("admin");
        user.setPassword(PasswordHasher.hash("oldpass123"));
        user.setUserLevel(UserLevel.ADMIN);
        user.setMustChangePassword(true);
        secCtx = mock(SecurityContext.class);
    }

    @Test
    void loginSetsHttpOnlySessionCookie() {
        when(authService.authenticate("admin", "oldpass123"))
                .thenReturn(new AuthService.AuthResult("jwt-123", user));
        when(authService.getExpirationSeconds()).thenReturn(28800L);
        when(secCtx.isSecure()).thenReturn(true);

        Response resp = authResource.login(new LoginRequest("admin", "oldpass123"), secCtx);

        assertThat(resp.getStatus()).isEqualTo(200);
        LoginResponse body = (LoginResponse) resp.getEntity();
        assertThat(body.token()).isEqualTo("jwt-123");
        assertThat(body.user().mustChangePassword()).isTrue();
        String cookie = resp.getHeaderString(HttpHeaders.SET_COOKIE);
        assertThat(cookie).contains("periscope_token=jwt-123")
                .contains("HttpOnly")
                .contains("SameSite=Lax")
                .contains("Path=/periscope")
                .contains("Secure");
    }

    @Test
    void logoutRevokesClaimsAndClearsCookie() {
        ContainerRequestContext reqCtx = mock(ContainerRequestContext.class);
        JwtService.JwtClaims claims = new JwtService.JwtClaims("admin", "ADMIN", 9999L, 1L, "jti-9");
        when(reqCtx.getProperty(JwtAuthFilter.CLAIMS_PROPERTY)).thenReturn(claims);

        Response resp = authResource.logout(secCtx, reqCtx);

        verify(authService).revoke(claims);
        assertThat(resp.getStatus()).isEqualTo(204);
        assertThat(resp.getHeaderString(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");
    }

    @Test
    void refreshRotatesTokenAndSetsNewCookie() {
        ContainerRequestContext reqCtx = mock(ContainerRequestContext.class);
        JwtService.JwtClaims claims = new JwtService.JwtClaims("admin", "ADMIN", 9999L, 1L, "jti-1");
        when(securitySupport.requireUser(secCtx)).thenReturn(user);
        when(reqCtx.getProperty(JwtAuthFilter.CLAIMS_PROPERTY)).thenReturn(claims);
        when(authService.refresh(user, claims))
                .thenReturn(new AuthService.AuthResult("jwt-new", user));
        when(authService.getExpirationSeconds()).thenReturn(3600L);

        Response resp = authResource.refresh(secCtx, reqCtx);

        assertThat(resp.getStatus()).isEqualTo(200);
        LoginResponse body = (LoginResponse) resp.getEntity();
        assertThat(body.token()).isEqualTo("jwt-new");
        assertThat(resp.getHeaderString(HttpHeaders.SET_COOKIE)).contains("periscope_token=jwt-new");
    }

    @Test
    void changePasswordValidatesAndClearsMustChangeFlag() {
        when(securitySupport.requireUser(secCtx)).thenReturn(user);

        assertThatThrownBy(() -> authResource.changePassword(
                new ChangePasswordRequest("oldpass123", "short"), secCtx))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(400));

        assertThatThrownBy(() -> authResource.changePassword(
                new ChangePasswordRequest("wrongpass", "newpass1234"), secCtx))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(400));

        UserDTO updated = authResource.changePassword(
                new ChangePasswordRequest("oldpass123", "newStrongPass9"), secCtx);

        verify(ds).save(user);
        assertThat(updated.mustChangePassword()).isFalse();
        assertThat(PasswordHasher.verify("newStrongPass9", user.getPassword())).isTrue();
    }
}
