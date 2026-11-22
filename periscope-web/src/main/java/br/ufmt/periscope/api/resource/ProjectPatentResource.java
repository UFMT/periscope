package br.ufmt.periscope.api.resource;

import br.ufmt.periscope.api.ApiException;
import br.ufmt.periscope.api.dto.ImportResultDTO;
import br.ufmt.periscope.api.dto.PageDTO;
import br.ufmt.periscope.api.dto.PatentCreateRequest;
import br.ufmt.periscope.api.dto.PatentDTO;
import br.ufmt.periscope.api.security.ApiSecuritySupport;
import br.ufmt.periscope.importer.PatentImporter;
import br.ufmt.periscope.importer.PatentImporterFactory;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.User;
import br.ufmt.periscope.repository.PatentRepository;
import dev.morphia.Datastore;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
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
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

@Path("/projects/{projectId}/patents")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
@RolesAllowed({"ADMIN", "USER"})
@Tag(name = "Patents")
@SecurityRequirement(name = "bearerAuth")
public class ProjectPatentResource {

    @Inject
    private Datastore ds;

    @Inject
    private PatentRepository patentRepository;

    @Inject
    private PatentImporterFactory importerFactory;

    @Inject
    private ApiSecuritySupport securitySupport;

    @Inject
    private PatentMapperSupport patentMapper;

    @GET
    @Operation(summary = "Listar patentes do projeto (paginado)")
    public PageDTO<PatentDTO> list(
            @PathParam("projectId") String projectId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("q") String q,
            @QueryParam("country") String country,
            @QueryParam("blacklisted") @DefaultValue("false") boolean blacklisted,
            @Context SecurityContext securityContext) {
        User user = securitySupport.requireUser(securityContext);
        Project project = securitySupport.requireProject(projectId, user);
        if (page < 0) {
            page = 0;
        }
        if (size <= 0 || size > 200) {
            size = 20;
        }
        patentRepository.setCurrentProject(project);
        patentRepository.setBlacklisted(blacklisted);
        patentRepository.setCompleted(null);
        Map<String, String> filters = new HashMap<>();
        if (q != null && !q.isBlank()) {
            filters.put("titleSelect", q.trim());
        }
        if (country != null && !country.isBlank()) {
            filters.put("applicationCountry.acronym", country.trim());
        }
        List<Patent> patents = patentRepository.load(page * size, size, "publicationDate", 1, filters);
        long total = patentRepository.getRowCount();
        return PageDTO.of(
                patents.stream().map(p -> PatentDTO.from(p, projectId)).toList(),
                page, size, total);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Cadastrar patente manualmente")
    public Response create(
            @PathParam("projectId") String projectId,
            PatentCreateRequest request,
            @Context SecurityContext securityContext) {
        User user = securitySupport.requireUser(securityContext);
        Project project = securitySupport.requireProject(projectId, user);
        if (request == null || request.title() == null || request.title().isBlank()) {
            throw ApiException.badRequest("title is required");
        }
        boolean hasPub = request.publicationNumber() != null && !request.publicationNumber().isBlank();
        boolean hasApp = request.applicationNumber() != null && !request.applicationNumber().isBlank();
        if (!hasPub && !hasApp) {
            throw ApiException.badRequest("publicationNumber or applicationNumber is required");
        }

        Patent patent = new Patent();
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
                request.blacklisted() != null ? request.blacklisted() : Boolean.FALSE,
                request.completed());
        patent.setProject(project);

        if (patentRepository.patentExistsForProject(patent, project)) {
            throw ApiException.conflict("Patent already exists in this project");
        }
        patentRepository.savePatentToDatabase(patent, project);
        if (request.completed() != null && !request.completed().equals(patent.getCompleted())) {
            patent.setCompleted(request.completed());
            ds.save(patent);
        }

        PatentDTO dto = PatentDTO.from(patent, projectId);
        return Response.created(URI.create("/periscope/rest/patents/" + dto.id()))
                .entity(dto)
                .build();
    }

    @POST
    @Path("/import")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Importar patentes", description = "multipart: file + importer (ESPACENET|PATENTSCOPE|DPMA)")
    public ImportResultDTO importPatents(
            @PathParam("projectId") String projectId,
            MultipartFormDataInput input,
            @Context SecurityContext securityContext) throws Exception {
        User user = securitySupport.requireUser(securityContext);
        Project project = securitySupport.requireProject(projectId, user);
        if (project.getPatents() == null) {
            project.setPatents(new ArrayList<>());
        }

        Map<String, List<InputPart>> form = input.getFormDataMap();
        String importerType = firstFormValue(form, "importer");
        if (importerType == null) {
            importerType = firstFormValue(form, "type");
        }
        if (importerType == null || importerType.isBlank()) {
            throw ApiException.badRequest("Form field 'importer' is required (ESPACENET, PATENTSCOPE, DPMA)");
        }
        List<InputPart> fileParts = form.get("file");
        if (fileParts == null || fileParts.isEmpty()) {
            throw ApiException.badRequest("Form field 'file' is required");
        }
        InputPart filePart = fileParts.get(0);
        String fileName = extractFileName(filePart);
        long before = securitySupport.countPatents(project);
        try (InputStream is = filePart.getBody(InputStream.class, null)) {
            PatentImporter importer = importerFactory.getImporter(importerType.trim());
            if (!importer.initWithStream(is)) {
                throw ApiException.badRequest("Could not parse file with importer " + importerType);
            }
            patentRepository.savePatentToDatabase(importer, project);
        } catch (NoClassDefFoundError ex) {
            throw ApiException.badRequest(ex.getMessage());
        } catch (RuntimeException ex) {
            long afterError = securitySupport.countPatents(project);
            if (afterError > before) {
                List<String> messages = new ArrayList<>();
                messages.add("Imported with indexing warning: " + ex.getMessage());
                return new ImportResultDTO((int) (afterError - before), importerType.trim(), fileName, messages);
            }
            throw ex;
        }
        long after = securitySupport.countPatents(project);
        int imported = (int) Math.max(0, after - before);
        List<String> messages = new ArrayList<>();
        messages.add("Import finished");
        return new ImportResultDTO(imported, importerType.trim(), fileName, messages);
    }

    private static String firstFormValue(Map<String, List<InputPart>> form, String key) throws Exception {
        List<InputPart> parts = form.get(key);
        if (parts == null || parts.isEmpty()) {
            return null;
        }
        return parts.get(0).getBodyAsString();
    }

    private static String extractFileName(InputPart part) {
        String header = part.getHeaders().getFirst("Content-Disposition");
        if (header == null) {
            return "upload";
        }
        for (String token : header.split(";")) {
            if (token.trim().startsWith("filename")) {
                String[] name = token.split("=");
                if (name.length > 1) {
                    return name[1].trim().replace("\"", "");
                }
            }
        }
        return "upload";
    }
}
