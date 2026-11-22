package br.ufmt.periscope.api.filter;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * CORS for the SPA (default origin {@code http://localhost:5173}).
 * Configurable via {@code PERISCOPE_CORS_ORIGINS} (comma-separated).
 */
@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String DEFAULT_ORIGIN = "http://localhost:5173";
    private static final String ALLOW_METHODS = "GET, POST, PUT, DELETE, OPTIONS";
    private static final String ALLOW_HEADERS = "Authorization, Content-Type";

    private final List<String> allowedOrigins;

    public CorsFilter() {
        String env = System.getenv("PERISCOPE_CORS_ORIGINS");
        if (env == null || env.isBlank()) {
            allowedOrigins = List.of(DEFAULT_ORIGIN);
        } else {
            allowedOrigins = Arrays.stream(env.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(requestContext.getMethod())) {
            requestContext.abortWith(Response.noContent().build());
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        String origin = requestContext.getHeaderString("Origin");
        if (origin != null && isAllowed(origin)) {
            responseContext.getHeaders().putSingle("Access-Control-Allow-Origin", origin);
            responseContext.getHeaders().putSingle("Vary", "Origin");
            responseContext.getHeaders().putSingle("Access-Control-Allow-Credentials", "true");
        } else if (origin == null && allowedOrigins.size() == 1) {
            responseContext.getHeaders().putSingle("Access-Control-Allow-Origin", allowedOrigins.get(0));
        }
        responseContext.getHeaders().putSingle("Access-Control-Allow-Methods", ALLOW_METHODS);
        responseContext.getHeaders().putSingle("Access-Control-Allow-Headers", ALLOW_HEADERS);
        responseContext.getHeaders().putSingle("Access-Control-Expose-Headers", "Content-Disposition");
        responseContext.getHeaders().putSingle("Access-Control-Max-Age", "86400");
    }

    private boolean isAllowed(String origin) {
        if (allowedOrigins.contains("*")) {
            return true;
        }
        return allowedOrigins.contains(origin);
    }
}
