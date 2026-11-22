package br.ufmt.periscope.api.resource;

import br.ufmt.periscope.api.ApiException;
import br.ufmt.periscope.api.dto.HarmonizationEntityDTO;
import br.ufmt.periscope.api.dto.PageDTO;
import br.ufmt.periscope.api.dto.RuleDTO;
import br.ufmt.periscope.api.dto.RuleRequest;
import br.ufmt.periscope.api.security.ApiSecuritySupport;
import br.ufmt.periscope.harmonization.Harmonization;
import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.model.ApplicantType;
import br.ufmt.periscope.model.Country;
import br.ufmt.periscope.model.Inventor;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.Rule;
import br.ufmt.periscope.model.RuleType;
import br.ufmt.periscope.model.State;
import br.ufmt.periscope.model.User;
import br.ufmt.periscope.repository.ApplicantRepository;
import br.ufmt.periscope.repository.ApplicantTypeRepository;
import br.ufmt.periscope.repository.CountryRepository;
import br.ufmt.periscope.repository.InventorRepository;
import br.ufmt.periscope.repository.RuleRepository;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/projects/{projectId}/harmonization")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
@RolesAllowed({"ADMIN", "USER"})
@Tag(name = "Harmonization")
@SecurityRequirement(name = "bearerAuth")
public class HarmonizationResource {

    @Inject
    private ApiSecuritySupport securitySupport;

    @Inject
    private ApplicantRepository applicantRepository;

    @Inject
    private InventorRepository inventorRepository;

    @Inject
    private RuleRepository ruleRepository;

    @Inject
    private Harmonization harmonization;

    @Inject
    private CountryRepository countryRepository;

    @Inject
    private ApplicantTypeRepository applicantTypeRepository;

    @GET
    @Path("/suggestions")
    @Operation(summary = "Sugestões Lucene Fast-Join")
    public Map<String, Object> suggestions(
            @PathParam("projectId") String projectId,
            @QueryParam("type") @DefaultValue("applicant") String type,
            @QueryParam("query") String query,
            @QueryParam("top") @DefaultValue("50") int top,
            @Context SecurityContext securityContext) {
        User user = securitySupport.requireUser(securityContext);
        Project project = securitySupport.requireProject(projectId, user);
        if (query == null || query.isBlank()) {
            throw ApiException.badRequest("query is required");
        }
        if (top <= 0 || top > 500) {
            top = 50;
        }
        Set<String> suggestions;
        String kind = type.trim().toLowerCase();
        if ("inventor".equals(kind)) {
            suggestions = inventorRepository.getInventorSugestions(project, top, query.trim());
        } else if ("applicant".equals(kind)) {
            suggestions = applicantRepository.getApplicantSugestions(project, top, query.trim());
        } else {
            throw ApiException.badRequest("type must be applicant or inventor");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", kind);
        body.put("query", query.trim());
        body.put("suggestions", new ArrayList<>(suggestions));
        return body;
    }

    @GET
    @Path("/entities")
    @Operation(summary = "Listar depositantes ou inventores do projeto")
    public PageDTO<HarmonizationEntityDTO> listEntities(
            @PathParam("projectId") String projectId,
            @QueryParam("type") @DefaultValue("applicant") String type,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("q") String q,
            @Context SecurityContext securityContext) {
        User user = securitySupport.requireUser(securityContext);
        Project project = securitySupport.requireProject(projectId, user);
        if (page < 0) {
            page = 0;
        }
        if (size <= 0 || size > 200) {
            size = 20;
        }
        String kind = type.trim().toLowerCase();
        Map<String, String> filters = (q != null && !q.isBlank()) ? Map.of("name", q.trim()) : Map.of();
        if ("applicant".equals(kind)) {
            applicantRepository.setCurrentProject(project);
            List<Applicant> items = applicantRepository.load(page * size, size, "documentCount", 1, filters);
            long total = applicantRepository.getCount();
            List<HarmonizationEntityDTO> dtos = items.stream()
                    .map(a -> new HarmonizationEntityDTO(
                            a.getName(),
                            a.getAcronym(),
                            a.getCountry() != null ? a.getCountry().getAcronym() : null,
                            a.getState() != null ? a.getState().getAcronym() : null,
                            a.getType() != null ? a.getType().getName() : null,
                            a.getHarmonized(),
                            a.getDocumentCount() != null ? a.getDocumentCount() : 0))
                    .toList();
            return PageDTO.of(dtos, page, size, total);
        }
        if ("inventor".equals(kind)) {
            inventorRepository.setCurrentProject(project);
            List<Inventor> items = inventorRepository.load(page * size, size, "documentCount", 1, filters);
            long total = inventorRepository.getCount();
            List<HarmonizationEntityDTO> dtos = items.stream()
                    .map(i -> new HarmonizationEntityDTO(
                            i.getName(),
                            i.getAcronym(),
                            i.getCountry() != null ? i.getCountry().getAcronym() : null,
                            i.getState() != null ? i.getState().getAcronym() : null,
                            null,
                            i.getHarmonized(),
                            i.getDocumentCount() != null ? i.getDocumentCount() : 0))
                    .toList();
            return PageDTO.of(dtos, page, size, total);
        }
        throw ApiException.badRequest("type must be applicant or inventor");
    }

    @GET
    @Path("/rules")
    @Operation(summary = "Listar regras de harmonização")
    public List<RuleDTO> listRules(
            @PathParam("projectId") String projectId,
            @QueryParam("type") String type,
            @Context SecurityContext securityContext) {
        User user = securitySupport.requireUser(securityContext);
        Project project = securitySupport.requireProject(projectId, user);
        List<Rule> rules;
        if (type == null || type.isBlank()) {
            rules = ruleRepository.getAllRule(project);
        } else if ("applicant".equalsIgnoreCase(type)) {
            rules = ruleRepository.getApplicantRule(project);
        } else if ("inventor".equalsIgnoreCase(type)) {
            rules = ruleRepository.getInventorRule(project);
        } else {
            throw ApiException.badRequest("type must be applicant or inventor");
        }
        return rules.stream().map(r -> RuleDTO.from(r, projectId)).toList();
    }

    @POST
    @Path("/rules")
    @Operation(summary = "Criar regra")
    public Response createRule(
            @PathParam("projectId") String projectId,
            RuleRequest request,
            @Context SecurityContext securityContext) {
        User user = securitySupport.requireUser(securityContext);
        Project project = securitySupport.requireProject(projectId, user);
        Rule rule = new Rule();
        rule.setProject(project);
        applyRuleFields(rule, request, true);
        ruleRepository.setCurrentProject(project);
        ruleRepository.save(rule);
        return Response.status(Response.Status.CREATED).entity(RuleDTO.from(rule, projectId)).build();
    }

    @PUT
    @Path("/rules/{ruleId}")
    @Operation(summary = "Atualizar regra")
    public RuleDTO updateRule(
            @PathParam("projectId") String projectId,
            @PathParam("ruleId") String ruleId,
            RuleRequest request,
            @Context SecurityContext securityContext) {
        User user = securitySupport.requireUser(securityContext);
        Project project = securitySupport.requireProject(projectId, user);
        Rule rule = loadProjectRule(ruleId, project);
        if (request == null) {
            throw ApiException.badRequest("Body is required");
        }
        rule.setProject(project);
        applyRuleFields(rule, request, false);
        ruleRepository.update(rule);
        return RuleDTO.from(rule, projectId);
    }

    @POST
    @Path("/rules/{ruleId}/undo")
    @Operation(summary = "Desfazer aplicação de uma regra")
    public Map<String, Object> undoRule(
            @PathParam("projectId") String projectId,
            @PathParam("ruleId") String ruleId,
            @Context SecurityContext securityContext) {
        User user = securitySupport.requireUser(securityContext);
        Project project = securitySupport.requireProject(projectId, user);
        Rule rule = loadProjectRule(ruleId, project);
        if (rule.getType() == RuleType.INVENTOR) {
            ruleRepository.undoInventorRule(project, rule.getName());
        } else {
            ruleRepository.undoApplicantRule(project, rule.getName());
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("undone", 1);
        body.put("ruleId", ruleId);
        body.put("message", "Rule undone successfully");
        return body;
    }

    @DELETE
    @Path("/rules/{ruleId}")
    @Operation(summary = "Excluir regra")
    public Response deleteRule(
            @PathParam("projectId") String projectId,
            @PathParam("ruleId") String ruleId,
            @Context SecurityContext securityContext) {
        User user = securitySupport.requireUser(securityContext);
        Project project = securitySupport.requireProject(projectId, user);
        loadProjectRule(ruleId, project);
        ruleRepository.delete(ruleId);
        return Response.noContent().build();
    }

    @POST
    @Path("/apply")
    @Operation(summary = "Aplicar todas as regras")
    public Map<String, Object> applyAll(
            @PathParam("projectId") String projectId,
            @Context SecurityContext securityContext) {
        User user = securitySupport.requireUser(securityContext);
        Project project = securitySupport.requireProject(projectId, user);
        List<Rule> rules = ruleRepository.getAllRule(project);
        int applied = 0;
        for (Rule rule : rules) {
            rule.setProject(project);
            harmonization.applyRule(rule);
            applied++;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("applied", applied);
        body.put("message", "Rules applied successfully");
        return body;
    }

    @POST
    @Path("/rules/{ruleId}/apply")
    @Operation(summary = "Aplicar uma regra")
    public Map<String, Object> applyOne(
            @PathParam("projectId") String projectId,
            @PathParam("ruleId") String ruleId,
            @Context SecurityContext securityContext) {
        User user = securitySupport.requireUser(securityContext);
        Project project = securitySupport.requireProject(projectId, user);
        Rule rule = loadProjectRule(ruleId, project);
        rule.setProject(project);
        harmonization.applyRule(rule);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("applied", 1);
        body.put("ruleId", ruleId);
        body.put("message", "Rule applied successfully");
        return body;
    }

    private Rule loadProjectRule(String ruleId, Project project) {
        Rule rule = ruleRepository.findById(ruleId);
        if (rule == null || rule.getProject() == null
                || !rule.getProject().getId().equals(project.getId())) {
            throw ApiException.notFound("Rule not found");
        }
        return rule;
    }

    private void applyRuleFields(Rule rule, RuleRequest request, boolean requireNameAndType) {
        if (requireNameAndType) {
            if (request == null || request.name() == null || request.name().isBlank()) {
                throw ApiException.badRequest("name is required");
            }
            if (request.type() == null) {
                throw ApiException.badRequest("type is required (APPLICANT or INVENTOR)");
            }
        }
        if (request.name() != null && !request.name().isBlank()) {
            rule.setName(request.name().trim().toUpperCase());
        }
        if (request.acronym() != null) {
            rule.setAcronym(request.acronym().isBlank() ? null : request.acronym().trim().toUpperCase());
        }
        if (request.type() != null && !request.type().isBlank()) {
            try {
                rule.setType(RuleType.valueOf(request.type().trim().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                throw ApiException.badRequest("type must be APPLICANT or INVENTOR");
            }
        }
        if (request.substitutions() != null) {
            rule.setSubstitutions(new HashSet<>(request.substitutions()));
        }
        if (request.countryAcronym() != null) {
            if (request.countryAcronym().isBlank()) {
                rule.setCountry(new Country());
                rule.setState(new State());
            } else {
                Country found = countryRepository.getCountryByAcronym(request.countryAcronym().trim().toUpperCase());
                if (found != null) {
                    Country copy = new Country();
                    copy.setId(found.getId());
                    copy.setAcronym(found.getAcronym());
                    copy.setName(found.getName());
                    copy.setStates(null);
                    rule.setCountry(copy);
                    rule.setState(resolveState(found, request.stateAcronym()));
                }
            }
        } else if (request.stateAcronym() != null) {
            if (request.stateAcronym().isBlank()) {
                rule.setState(new State());
            } else {
                Country existingCountry = rule.getCountry() != null && rule.getCountry().getAcronym() != null
                        ? countryRepository.getCountryByAcronym(rule.getCountry().getAcronym())
                        : null;
                rule.setState(resolveState(existingCountry, request.stateAcronym()));
            }
        }
        if (request.nature() != null) {
            if (request.nature().isBlank()) {
                rule.setNature(new ApplicantType());
            } else {
                for (ApplicantType at : applicantTypeRepository.getAll()) {
                    if (request.nature().trim().equalsIgnoreCase(at.getName())) {
                        rule.setNature(at);
                        break;
                    }
                }
            }
        }
    }

    private static State resolveState(Country country, String stateAcronym) {
        if (stateAcronym == null || stateAcronym.isBlank()) {
            return new State();
        }
        String code = stateAcronym.trim().toUpperCase();
        if (country != null && country.getStates() != null) {
            for (State s : country.getStates()) {
                if (s != null && code.equalsIgnoreCase(s.getAcronym())) {
                    State copy = new State();
                    copy.setAcronym(s.getAcronym());
                    copy.setName(s.getName());
                    copy.setRegion(s.getRegion());
                    return copy;
                }
            }
        }
        State fallback = new State();
        fallback.setAcronym(code);
        return fallback;
    }
}
