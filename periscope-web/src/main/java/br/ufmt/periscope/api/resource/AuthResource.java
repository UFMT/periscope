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
import br.ufmt.periscope.security.PasswordHasher;
import dev.morphia.Datastore;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
@Tag(name = "Auth")
public class AuthResource {

    private static final int MIN_PASSWORD_LENGTH = 8;

    @Inject
    private AuthService authService;

    @Inject
    private ApiSecuritySupport securitySupport;

    @Inject
    private Datastore ds;

    @POST
    @Path("/login")
    @PermitAll
    @Operation(summary = "Login", description = "Autentica e devolve JWT + usuário + cookie HttpOnly")
    @APIResponse(responseCode = "200", description = "Token emitido")
    @APIResponse(responseCode = "401", description = "Credenciais inválidas")
    public Response login(LoginRequest request, @Context SecurityContext securityContext) {
        if (request == null || request.username() == null || request.password() == null) {
            throw ApiException.badRequest("username and password are required");
        }
        AuthService.AuthResult result = authService.authenticate(request.username(), request.password());
        if (result == null) {
            throw ApiException.unauthorized("Invalid username or password");
        }
        String cookie = buildSessionCookie(result.token(), authService.getExpirationSeconds(), isCookieSecure(securityContext));
        return Response.ok(new LoginResponse(result.token(), UserDTO.from(result.user())))
                .header(HttpHeaders.SET_COOKIE, cookie)
                .build();
    }

    @GET
    @Path("/me")
    @RolesAllowed({"ADMIN", "USER"})
    @Operation(summary = "Usuário atual")
    @SecurityRequirement(name = "bearerAuth")
    public UserDTO me(@Context SecurityContext securityContext) {
        User user = securitySupport.requireUser(securityContext);
        return UserDTO.from(user);
    }

    @POST
    @Path("/refresh")
    @RolesAllowed({"ADMIN", "USER"})
    @Operation(summary = "Renovar sessão", description = "Revoga o token atual e emite um novo JWT")
    @SecurityRequirement(name = "bearerAuth")
    public Response refresh(
            @Context SecurityContext securityContext,
            @Context ContainerRequestContext requestContext) {
        User user = securitySupport.requireUser(securityContext);
        JwtService.JwtClaims claims = extractClaims(requestContext);
        AuthService.AuthResult result = authService.refresh(user, claims);
        String cookie = buildSessionCookie(result.token(), authService.getExpirationSeconds(), isCookieSecure(securityContext));
        return Response.ok(new LoginResponse(result.token(), UserDTO.from(result.user())))
                .header(HttpHeaders.SET_COOKIE, cookie)
                .build();
    }

    @POST
    @Path("/change-password")
    @RolesAllowed({"ADMIN", "USER"})
    @Operation(summary = "Alterar senha do usuário logado")
    @SecurityRequirement(name = "bearerAuth")
    public UserDTO changePassword(ChangePasswordRequest request, @Context SecurityContext securityContext) {
        User user = securitySupport.requireUser(securityContext);
        if (request == null || request.currentPassword() == null || request.newPassword() == null) {
            throw ApiException.badRequest("currentPassword and newPassword are required");
        }
        if (request.newPassword().length() < MIN_PASSWORD_LENGTH) {
            throw ApiException.badRequest("newPassword must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (!PasswordHasher.verify(request.currentPassword(), user.getPassword())) {
            throw ApiException.badRequest("Current password is incorrect");
        }
        user.setPassword(PasswordHasher.hash(request.newPassword()));
        user.setMustChangePassword(false);
        ds.save(user);
        return UserDTO.from(user);
    }

    /**
     * Revokes the active JWT (by {@code jti}) and clears the HttpOnly session cookie.
     */
    @POST
    @Path("/logout")
    @RolesAllowed({"ADMIN", "USER"})
    @Operation(summary = "Logout com revogação de token", description = "Revoga o JWT atual e limpa o cookie")
    @SecurityRequirement(name = "bearerAuth")
    @APIResponse(responseCode = "204", description = "Sem conteúdo")
    public Response logout(
            @Context SecurityContext securityContext,
            @Context ContainerRequestContext requestContext) {
        JwtService.JwtClaims claims = extractClaims(requestContext);
        authService.revoke(claims);
        String clearCookie = buildSessionCookie("", 0, isCookieSecure(securityContext));
        return Response.noContent()
                .header(HttpHeaders.SET_COOKIE, clearCookie)
                .build();
    }

    private static JwtService.JwtClaims extractClaims(ContainerRequestContext requestContext) {
        if (requestContext == null) {
            return null;
        }
        Object attr = requestContext.getProperty(JwtAuthFilter.CLAIMS_PROPERTY);
        return attr instanceof JwtService.JwtClaims c ? c : null;
    }

    private static boolean isCookieSecure(SecurityContext securityContext) {
        if ("true".equalsIgnoreCase(System.getenv("PERISCOPE_COOKIE_SECURE"))) {
            return true;
        }
        return securityContext != null && securityContext.isSecure();
    }

    static String buildSessionCookie(String token, long maxAgeSeconds, boolean secure) {
        StringBuilder sb = new StringBuilder();
        sb.append(JwtAuthFilter.TOKEN_COOKIE).append('=').append(token != null ? token : "");
        sb.append("; Path=/periscope; HttpOnly; SameSite=Lax; Max-Age=").append(maxAgeSeconds);
        if (secure) {
            sb.append("; Secure");
        }
        return sb.toString();
    }
}
