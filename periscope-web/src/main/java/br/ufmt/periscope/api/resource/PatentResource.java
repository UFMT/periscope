package br.ufmt.periscope.api.resource;

import br.ufmt.periscope.api.ApiException;
import br.ufmt.periscope.api.dto.PatentDTO;
import br.ufmt.periscope.api.dto.PatentUpdateRequest;
import br.ufmt.periscope.api.security.ApiSecuritySupport;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.User;
import br.ufmt.periscope.repository.PatentRepository;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import dev.morphia.Datastore;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import static dev.morphia.query.filters.Filters.eq;

@Path("/patents")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
@RolesAllowed({"ADMIN", "USER"})
@Tag(name = "Patents")
@SecurityRequirement(name = "bearerAuth")
public class PatentResource {

    @Inject
    private Datastore ds;

    @Inject
    private PatentRepository patentRepository;

    @Inject
    private ApiSecuritySupport securitySupport;

    @Inject
    private PatentMapperSupport patentMapper;

    @GET
    @Path("/{id}")
    @Operation(summary = "Obter patente")
    public PatentDTO get(@PathParam("id") String id, @Context SecurityContext securityContext) {
        User user = securitySupport.requireUser(securityContext);
        LoadedPatent loaded = findPatent(id);
        securitySupport.requireProject(loaded.projectId(), user);
        return PatentDTO.from(loaded.patent(), loaded.projectId());
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Atualizar patente")
    public PatentDTO update(@PathParam("id") String id, PatentUpdateRequest request,
                            @Context SecurityContext securityContext) {
        User user = securitySupport.requireUser(securityContext);
        LoadedPatent loaded = findPatent(id);
        securitySupport.requireProject(loaded.projectId(), user);
        if (request == null) {
            throw ApiException.badRequest("Body is required");
        }
        Patent patent = loaded.patent();
        patentMapper.applyFields(
                patent,
                request.title(),
                request.abstractText(),
                request.publicationNumber(),
                request.publicationDate(),
                request.applicationNumber(),
                request.applicationDate(),
                request.applicationCountry(),
                request.language(),
                request.mainClassification(),
                request.classifications(),
                request.applicants(),
                request.inventors(),
                request.priorities(),
                request.blacklisted(),
                request.completed());
        // Keep a stub project so Morphia save retains the DBRef
        Project stub = new Project();
        stub.setId(new ObjectId(loaded.projectId()));
        patent.setProject(stub);
        patentRepository.savePatent(patent);
        if (request.completed() != null && !request.completed().equals(patent.getCompleted())) {
            patent.setCompleted(request.completed());
            ds.save(patent);
        }
        return PatentDTO.from(patent, loaded.projectId());
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Excluir patente")
    public Response delete(@PathParam("id") String id, @Context SecurityContext securityContext) {
        User user = securitySupport.requireUser(securityContext);
        LoadedPatent loaded = findPatent(id);
        Project project = securitySupport.requireProject(loaded.projectId(), user);
        if (project.getPatents() != null) {
            ObjectId oid = loaded.patent().getId();
            project.getPatents().removeIf(p -> p.getId() != null && p.getId().equals(oid));
            ds.save(project);
        }
        ds.delete(loaded.patent());
        return Response.noContent().build();
    }

    /**
     * Loads the patent without resolving {@code @Reference project} via Morphia
     * (project id is read from the raw document).
     */
    private LoadedPatent findPatent(String id) {
        if (id == null || !ObjectId.isValid(id)) {
            throw ApiException.badRequest("Invalid patent id");
        }
        ObjectId oid = new ObjectId(id);
        String collection = ds.getMapper().getEntityModel(Patent.class).getCollectionName();
        Document raw = ds.getDatabase().getCollection(collection)
                .find(Filters.eq("_id", oid))
                .projection(Projections.include("project"))
                .first();
        if (raw == null) {
            throw ApiException.notFound("Patent not found");
        }
        String projectId = extractDbRefId(raw.get("project"));
        if (projectId == null) {
            throw ApiException.notFound("Patent has no project");
        }
        Patent patent = ds.find(Patent.class)
                .filter(eq("_id", oid))
                .first(new dev.morphia.query.FindOptions().projection().exclude("project"));
        if (patent == null) {
            throw ApiException.notFound("Patent not found");
        }
        Project stub = new Project();
        stub.setId(new ObjectId(projectId));
        patent.setProject(stub);
        return new LoadedPatent(patent, projectId);
    }

    private static String extractDbRefId(Object ref) {
        if (ref == null) {
            return null;
        }
        if (ref instanceof com.mongodb.DBRef dbRef) {
            Object id = dbRef.getId();
            return id != null ? id.toString() : null;
        }
        if (ref instanceof Document doc) {
            Object id = doc.get("$id");
            if (id == null) {
                id = doc.get("_id");
            }
            return id != null ? id.toString() : null;
        }
        if (ref instanceof ObjectId oid) {
            return oid.toString();
        }
        return null;
    }

    private record LoadedPatent(Patent patent, String projectId) {
    }
}
