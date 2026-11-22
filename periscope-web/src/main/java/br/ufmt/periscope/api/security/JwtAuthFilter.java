package br.ufmt.periscope.api.security;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.security.Principal;
import java.util.Map;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class JwtAuthFilter implements ContainerRequestFilter {

    public static final String TOKEN_COOKIE = "periscope_token";
    public static final String CLAIMS_PROPERTY = "jwt.claims";

    @Inject
    private AuthService authService;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(requestContext.getMethod())) {
            return;
        }
        String path = requestContext.getUriInfo().getPath();
        if (path != null) {
            String normalized = path.startsWith("/") ? path.substring(1) : path;
            if (("auth/login".equals(normalized) && "POST".equalsIgnoreCase(requestContext.getMethod()))
                    || ("health".equals(normalized) && "GET".equalsIgnoreCase(requestContext.getMethod()))) {
                return;
            }
        }

        String token = resolveToken(requestContext);
        if (token == null || token.isBlank()) {
            abortUnauthorized(requestContext, "Missing or invalid Authorization header");
            return;
        }
        try {
            JwtService.JwtClaims claims = authService.validate(token);
            final String username = claims.subject();
            final String role = normalizeRole(claims.userLevel());
            requestContext.setProperty("jwt.username", username);
            requestContext.setProperty("jwt.userLevel", role);
            requestContext.setProperty(CLAIMS_PROPERTY, claims);
            requestContext.setSecurityContext(new SecurityContext() {
                @Override
                public Principal getUserPrincipal() {
                    return () -> username;
                }

                @Override
                public boolean isUserInRole(String r) {
                    if (r == null) {
                        return false;
                    }
                    if (role.equalsIgnoreCase(r)) {
                        return true;
                    }
                    // ADMIN implies USER for read endpoints annotated with USER
                    return "USER".equalsIgnoreCase(r) && "ADMIN".equalsIgnoreCase(role);
                }

                @Override
                public boolean isSecure() {
                    return requestContext.getSecurityContext() != null
                            && requestContext.getSecurityContext().isSecure();
                }

                @Override
                public String getAuthenticationScheme() {
                    return "Bearer";
                }
            });
        } catch (IllegalArgumentException ex) {
            abortUnauthorized(requestContext, ex.getMessage());
        }
    }

    private static String resolveToken(ContainerRequestContext requestContext) {
        String header = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return header.substring(7).trim();
        }
        Map<String, Cookie> cookies = requestContext.getCookies();
        if (cookies != null) {
            Cookie tokenCookie = cookies.get(TOKEN_COOKIE);
            if (tokenCookie != null && tokenCookie.getValue() != null && !tokenCookie.getValue().isBlank()) {
                return tokenCookie.getValue().trim();
            }
        }
        return null;
    }

    private static String normalizeRole(String userLevel) {
        if (userLevel == null) {
            return "USER";
        }
        return userLevel.trim().toUpperCase();
    }

    private static void abortUnauthorized(ContainerRequestContext ctx, String message) {
        ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .entity(Map.of("error", message, "status", 401))
                .type("application/json")
                .build());
    }
}
