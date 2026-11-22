package br.ufmt.periscope.api.resource;

import br.ufmt.periscope.api.ApiException;
import br.ufmt.periscope.api.dto.ProjectDTO;
import br.ufmt.periscope.api.dto.ProjectRequest;
import br.ufmt.periscope.api.security.ApiSecuritySupport;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.User;
import br.ufmt.periscope.model.UserLevel;
import br.ufmt.periscope.repository.ProjectRepository;
import dev.morphia.Datastore;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import static dev.morphia.query.filters.Filters.eq;

@Path("/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
@RolesAllowed({"ADMIN", "USER"})
@Tag(name = "Projects")
@SecurityRequirement(name = "bearerAuth")
public class ProjectResource {

    @Inject
    private Datastore ds;

    @Inject
    private ProjectRepository projectRepository;

    @Inject
    private ApiSecuritySupport securitySupport;

    @GET
    @Operation(summary = "Listar projetos")
    public List<ProjectDTO> list(@Context SecurityContext securityContext) {
        User user = securitySupport.requireUser(securityContext);
        return projectRepository.getProjectList(user).stream()
                .map(p -> ProjectDTO.from(p, securitySupport.countPatents(p)))
                .toList();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Obter projeto")
    public ProjectDTO get(@PathParam("id") String id, @Context SecurityContext securityContext) {
        User user = securitySupport.requireUser(securityContext);
        Project project = securitySupport.requireProject(id, user);
        return ProjectDTO.from(project, securitySupport.countPatents(project));
    }

    @POST
    @Operation(summary = "Criar projeto")
    public Response create(ProjectRequest request, @Context SecurityContext securityContext) {
        User user = securitySupport.requireUser(securityContext);
        if (request == null || request.title() == null || request.title().isBlank()) {
            throw ApiException.badRequest("title is required");
        }
        Project project = new Project();
        project.setTitle(request.title().trim());
        project.setDescription(request.description());
        project.setIsPublic(request.isPublic() != null ? request.isPublic() : Boolean.FALSE);
        project.setCreatedAt(new Date());
        project.setUpdateAt(new Date());
        project.setOwner(user);
        if (request.observerIds() != null) {
            project.setObservers(resolveObservers(request.observerIds(), user));
        }
        ds.save(project);
        ProjectDTO dto = ProjectDTO.from(project, 0);
        return Response.created(URI.create("/periscope/rest/projects/" + dto.id()))
                .entity(dto)
                .build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Atualizar projeto")
    public ProjectDTO update(@PathParam("id") String id, ProjectRequest request,
                             @Context SecurityContext securityContext) {
        User user = securitySupport.requireUser(securityContext);
        Project project = securitySupport.requireProject(id, user);
        requireManager(project, user);
        if (request == null) {
            throw ApiException.badRequest("Body is required");
        }
        if (request.title() != null && !request.title().isBlank()) {
            project.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            project.setDescription(request.description());
        }
        if (request.isPublic() != null) {
            project.setIsPublic(request.isPublic());
        }
        if (request.observerIds() != null) {
            project.setObservers(resolveObservers(request.observerIds(), project.getOwner()));
        }
        project.setUpdateAt(new Date());
        ds.save(project);
        return ProjectDTO.from(project, securitySupport.countPatents(project));
    }

    @POST
    @Path("/{id}/observers/{userId}")
    @Operation(summary = "Adicionar observador ao projeto")
    public ProjectDTO addObserver(
            @PathParam("id") String id,
            @PathParam("userId") String userId,
            @Context SecurityContext securityContext) {
        User actor = securitySupport.requireUser(securityContext);
        Project project = securitySupport.requireProject(id, actor);
        requireManager(project, actor);
        User observer = findUserById(userId);
        if (project.getOwner() != null && observer.getId().equals(project.getOwner().getId())) {
            throw ApiException.badRequest("Project owner cannot be added as observer");
        }
        List<User> observers = project.getObservers() != null
                ? new ArrayList<>(project.getObservers())
                : new ArrayList<>();
        boolean alreadyPresent = observers.stream()
                .anyMatch(u -> u != null && observer.getId().equals(u.getId()));
        if (!alreadyPresent) {
            observers.add(observer);
            project.setObservers(observers);
            project.setUpdateAt(new Date());
            ds.save(project);
        }
        return ProjectDTO.from(project, securitySupport.countPatents(project));
    }

    @DELETE
    @Path("/{id}/observers/{userId}")
    @Operation(summary = "Remover observador do projeto")
    public ProjectDTO removeObserver(
            @PathParam("id") String id,
            @PathParam("userId") String userId,
            @Context SecurityContext securityContext) {
        User actor = securitySupport.requireUser(securityContext);
        Project project = securitySupport.requireProject(id, actor);
        requireManager(project, actor);
        if (userId == null || !ObjectId.isValid(userId)) {
            throw ApiException.badRequest("Invalid user id");
        }
        ObjectId oid = new ObjectId(userId);
        if (project.getObservers() != null) {
            project.getObservers().removeIf(u -> u != null && oid.equals(u.getId()));
            project.setUpdateAt(new Date());
            ds.save(project);
        }
        return ProjectDTO.from(project, securitySupport.countPatents(project));
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Excluir projeto")
    public Response delete(@PathParam("id") String id, @Context SecurityContext securityContext) {
        User user = securitySupport.requireUser(securityContext);
        Project project = securitySupport.requireProject(id, user);
        requireManager(project, user);
        projectRepository.deleteProject(id);
        return Response.noContent().build();
    }

    private static void requireManager(Project project, User user) {
        if (user.getUserLevel() == UserLevel.ADMIN) {
            return;
        }
        if (project.getOwner() == null || !project.getOwner().getId().equals(user.getId())) {
            throw ApiException.forbidden("Only the owner or an admin can modify this project");
        }
    }

    private List<User> resolveObservers(List<String> observerIds, User owner) {
        Set<String> seen = new LinkedHashSet<>();
        List<User> resolved = new ArrayList<>();
        String ownerId = owner != null && owner.getId() != null ? owner.getId().toString() : null;
        for (String rawId : observerIds) {
            if (rawId == null || rawId.isBlank()) {
                continue;
            }
            String trimmed = rawId.trim();
            if (trimmed.equals(ownerId) || !seen.add(trimmed)) {
                continue;
            }
            resolved.add(findUserById(trimmed));
        }
        return resolved;
    }

    private User findUserById(String userId) {
        if (userId == null || !ObjectId.isValid(userId)) {
            throw ApiException.badRequest("Invalid observer user id");
        }
        User user = ds.find(User.class).filter(eq("_id", new ObjectId(userId))).first();
        if (user == null) {
            throw ApiException.notFound("Observer user not found");
        }
        return user;
    }
}
