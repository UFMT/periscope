package br.ufmt.periscope.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeIn;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * JAX-RS activator. Resources are served under {@code /periscope/rest/*}.
 * Role checks ({@code @RolesAllowed}) are enabled via
 * {@code resteasy.role.based.security} in {@code web.xml}.
 * <p>
 * OpenAPI 3 document (WildFly MicroProfile OpenAPI): {@code GET /openapi}.
 */
@ApplicationPath("/rest")
@OpenAPIDefinition(
        info = @Info(
                title = "Periscope API",
                version = "2.1",
                description = "API REST JSON do Periscope (UFMT) para autenticação JWT, "
                        + "projetos, patentes, importação, harmonização, relatórios, "
                        + "usuários, dados de referência e arquivos GridFS. Health em /rest/health.",
                contact = @Contact(name = "UFMT / Periscope")
        ),
        servers = {
                @Server(url = "/periscope", description = "Context root local / Docker")
        },
        tags = {
                @Tag(name = "Auth", description = "Login JWT, refresh, troca de senha e logout"),
                @Tag(name = "Projects", description = "CRUD de projetos e observadores"),
                @Tag(name = "Patents", description = "Patentes, cadastro manual e importação"),
                @Tag(name = "Harmonization", description = "Sugestões Lucene, entidades e regras"),
                @Tag(name = "Reports", description = "13 relatórios agregados com filtros"),
                @Tag(name = "Reference", description = "Dados de referência (países, naturezas, usuários)"),
                @Tag(name = "Users", description = "Administração de usuários"),
                @Tag(name = "Files", description = "Anexos GridFS"),
                @Tag(name = "Health", description = "Liveness / readiness")
        }
)
@SecurityScheme(
        securitySchemeName = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        description = "JWT obtido em POST /rest/auth/login. Header: Authorization: Bearer <token>"
)
public class JaxRsActivator extends Application {
    /* empty: classpath scanning discovers resources/providers */
}
