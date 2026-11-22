package br.ufmt.periscope.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Serves {@code /app/index.html} for client-side routes of the React SPA
 * when the requested path under {@code /app/} is not an existing static asset.
 * Does not interfere with {@code /rest/*}.
 */
@WebFilter(urlPatterns = {"/app/*"})
public class SpaFallbackFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String path = req.getRequestURI().substring(req.getContextPath().length());

        if (isStaticAsset(path) || path.equals("/app") || path.equals("/app/") || path.equals("/app/index.html")) {
            chain.doFilter(request, response);
            return;
        }

        String resourcePath = path.startsWith("/") ? path : "/" + path;
        if (req.getServletContext().getResource(resourcePath) != null) {
            chain.doFilter(request, response);
            return;
        }

        RequestDispatcher dispatcher = req.getRequestDispatcher("/app/index.html");
        dispatcher.forward(request, response);
    }

    private static boolean isStaticAsset(String path) {
        int dot = path.lastIndexOf('.');
        if (dot < 0) {
            return false;
        }
        String ext = path.substring(dot + 1).toLowerCase();
        return ext.equals("js")
                || ext.equals("css")
                || ext.equals("map")
                || ext.equals("ico")
                || ext.equals("png")
                || ext.equals("jpg")
                || ext.equals("jpeg")
                || ext.equals("gif")
                || ext.equals("svg")
                || ext.equals("webp")
                || ext.equals("woff")
                || ext.equals("woff2")
                || ext.equals("ttf")
                || ext.equals("eot")
                || ext.equals("json")
                || ext.equals("txt")
                || ext.equals("html");
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }
}
