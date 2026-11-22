package br.ufmt.periscope.api.resource;

import br.ufmt.periscope.api.ApiException;
import br.ufmt.periscope.api.dto.PatentDTO;
import br.ufmt.periscope.api.security.ApiSecuritySupport;
import br.ufmt.periscope.model.Files;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.User;
import br.ufmt.periscope.model.UserLevel;
import br.ufmt.periscope.repository.PatentRepository;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import static dev.morphia.query.filters.Filters.eq;

@Path("/files")
@RequestScoped
@RolesAllowed({"ADMIN", "USER"})
@Tag(name = "Files")
@SecurityRequirement(name = "bearerAuth")
public class FileResource {

    @Inject
    private GridFSBucket gridFSBucket;

    @Inject
    private ApiSecuritySupport securitySupport;

    @Inject
    private PatentRepository patentRepository;

    @Inject
    private dev.morphia.Datastore ds;

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(summary = "Download de arquivo GridFS")
    public Response download(@PathParam("id") String id, @Context SecurityContext securityContext) {
        User user = securitySupport.requireUser(securityContext);
        if (id == null || !ObjectId.isValid(id)) {
            throw ApiException.badRequest("Invalid file id");
        }
        ObjectId oid = new ObjectId(id);
        String projectId = patentRepository.findProjectIdByFileId(oid);
        if (projectId != null) {
            securitySupport.requireProject(projectId, user);
        } else if (user.getUserLevel() != UserLevel.ADMIN) {
            throw ApiException.notFound("File not found");
        }

        GridFSFile gfile = gridFSBucket.find(Filters.eq("_id", oid)).first();
        if (gfile == null) {
            throw ApiException.notFound("File not found");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        gridFSBucket.downloadToStream(oid, out);
        String rawFilename = gfile.getFilename() != null ? gfile.getFilename() : id;
        String filename = rawFilename.replaceAll("[\\r\\n\"\\\\]", "_");
        String contentType = MediaType.APPLICATION_OCTET_STREAM;
        if (gfile.getMetadata() != null && gfile.getMetadata().getString("contentType") != null) {
            contentType = gfile.getMetadata().getString("contentType");
        }
        return Response.ok(out.toByteArray())
                .type(contentType)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .build();
    }

    /**
     * Upload a patent attachment ({@code kind=presentation|patentInfo}).
     */
    @POST
    @Path("/patents/{patentId}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Upload de anexo de patente", description = "kind=presentation|patentInfo")
    public PatentDTO uploadPatentFile(
            @PathParam("patentId") String patentId,
            @QueryParam("kind") String kind,
            MultipartFormDataInput input,
            @Context SecurityContext securityContext) throws Exception {
        User user = securitySupport.requireUser(securityContext);
        if (patentId == null || !ObjectId.isValid(patentId)) {
            throw ApiException.badRequest("Invalid patent id");
        }
        ObjectId oid = new ObjectId(patentId);
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
        securitySupport.requireProject(projectId, user);

        Patent patent = ds.find(Patent.class)
                .filter(eq("_id", oid))
                .first(new dev.morphia.query.FindOptions().projection().exclude("project"));
        if (patent == null) {
            throw ApiException.notFound("Patent not found");
        }
        Project stub = new Project();
        stub.setId(new ObjectId(projectId));
        patent.setProject(stub);

        String attachmentKind = kind != null ? kind.trim().toLowerCase() : "presentation";
        if (!"presentation".equals(attachmentKind) && !"patentinfo".equals(attachmentKind)
                && !"patentInfo".equalsIgnoreCase(kind)) {
            throw ApiException.badRequest("kind must be presentation or patentInfo");
        }
        Map<String, List<InputPart>> form = input.getFormDataMap();
        List<InputPart> fileParts = form.get("file");
        if (fileParts == null || fileParts.isEmpty()) {
            throw ApiException.badRequest("Form field 'file' is required");
        }
        InputPart filePart = fileParts.get(0);
        String filename = "upload";
        String header = filePart.getHeaders().getFirst("Content-Disposition");
        if (header != null) {
            for (String token : header.split(";")) {
                if (token.trim().startsWith("filename")) {
                    String[] name = token.split("=");
                    if (name.length > 1) {
                        filename = name[1].trim().replace("\"", "");
                    }
                }
            }
        }
        String contentType = filePart.getMediaType() != null
                ? filePart.getMediaType().toString()
                : MediaType.APPLICATION_OCTET_STREAM;
        try (InputStream is = filePart.getBody(InputStream.class, null)) {
            ObjectId newId = gridFSBucket.uploadFromStream(
                    filename,
                    is,
                    new GridFSUploadOptions().metadata(new Document("contentType", contentType)));
            Files files = new Files(newId);
            files.setFilename(filename);
            files.setContentType(contentType);
            if ("patentinfo".equals(attachmentKind) || "patentInfo".equalsIgnoreCase(kind)) {
                if (patent.getPatentInfo() != null && patent.getPatentInfo().getId() != null) {
                    try {
                        gridFSBucket.delete(patent.getPatentInfo().getId());
                    } catch (RuntimeException ignored) {
                        // best-effort
                    }
                }
                patent.setPatentInfo(files);
            } else {
                if (patent.getPresentationFile() != null && patent.getPresentationFile().getId() != null) {
                    try {
                        gridFSBucket.delete(patent.getPresentationFile().getId());
                    } catch (RuntimeException ignored) {
                        // best-effort
                    }
                }
                patent.setPresentationFile(files);
            }
            patentRepository.savePatent(patent);
        }
        return PatentDTO.from(patent, projectId);
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
        if (ref instanceof ObjectId objectId) {
            return objectId.toString();
        }
        return null;
    }
}
