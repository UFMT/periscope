package br.ufmt.periscope.api.resource;

import br.ufmt.periscope.api.ApiException;
import br.ufmt.periscope.api.dto.PageDTO;
import br.ufmt.periscope.api.dto.PatentDTO;
import br.ufmt.periscope.api.dto.ReportDTO;
import br.ufmt.periscope.api.dto.ReportItemDTO;
import br.ufmt.periscope.api.security.ApiSecuritySupport;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.User;
import br.ufmt.periscope.report.ApplicationDateReport;
import br.ufmt.periscope.report.ChartSeries;
import br.ufmt.periscope.report.MainApplicantReport;
import br.ufmt.periscope.report.MainIPCReport;
import br.ufmt.periscope.report.MainInventorReport;
import br.ufmt.periscope.report.MainNatureApplicantReport;
import br.ufmt.periscope.report.PriorityCountryReport;
import br.ufmt.periscope.report.PriorityDateReport;
import br.ufmt.periscope.report.PublicationDateReport;
import br.ufmt.periscope.report.RegionDistribuitionReport;
import br.ufmt.periscope.report.StateDistribuitionReport;
import br.ufmt.periscope.repository.ApplicantRepository;
import br.ufmt.periscope.repository.InventorRepository;
import br.ufmt.periscope.repository.PatentRepository;
import br.ufmt.periscope.util.Filters;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/projects/{projectId}/reports")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
@RolesAllowed({"ADMIN", "USER"})
@Tag(name = "Reports")
@SecurityRequirement(name = "bearerAuth")
public class ReportResource {

    private static final Map<Character, String> IPC_SECTION_DESCRIPTIONS = Map.of(
            'A', "Necessidades Humanas",
            'B', "Operações de Processamento; Transporte",
            'C', "Química e Metalurgia",
            'D', "Têxteis e Papel",
            'E', "Construções Fixas",
            'F', "Engenharia Mecânica; Iluminação; Aquecimento; Arma",
            'G', "Física",
            'H', "Eletricidade",
            'Y', "Novas Tecnologias"
    );

    @Inject
    private ApiSecuritySupport securitySupport;

    @Inject
    private PatentRepository patentRepository;

    @Inject
    private ApplicantRepository applicantRepository;

    @Inject
    private InventorRepository inventorRepository;

    @Inject
    private MainApplicantReport mainApplicantReport;

    @Inject
    private MainInventorReport mainInventorReport;

    @Inject
    private MainIPCReport mainIPCReport;

    @Inject
    private ApplicationDateReport applicationDateReport;

    @Inject
    private PublicationDateReport publicationDateReport;

    @Inject
    private PriorityCountryReport priorityCountryReport;

    @Inject
    private PriorityDateReport priorityDateReport;

    @Inject
    private MainNatureApplicantReport mainNatureApplicantReport;

    @Inject
    private StateDistribuitionReport stateDistribuitionReport;

    @Inject
    private RegionDistribuitionReport regionDistribuitionReport;

    @GET
    @Path("/main-applicant")
    @Operation(summary = "Principais depositantes")
    public ReportDTO mainApplicant(
            @PathParam("projectId") String projectId,
            @QueryParam("limit") @DefaultValue("10") int limit,
            @QueryParam("dateType") String dateType,
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate,
            @QueryParam("complete") @DefaultValue("false") boolean complete,
            @QueryParam("applicantType") String applicantType,
            @Context SecurityContext securityContext) {
        Project project = loadProject(projectId, securityContext);
        Filters f = filtersFor(1, dateType, startDate, endDate, complete, applicantType, null, null);
        ChartSeries series = mainApplicantReport.mainApplicantSeries(project, clamp(limit), f);
        return toReport("main-applicant", series, false);
    }

    @GET
    @Path("/main-inventor")
    @Operation(summary = "Principais inventores")
    public ReportDTO mainInventor(
            @PathParam("projectId") String projectId,
            @QueryParam("limit") @DefaultValue("10") int limit,
            @QueryParam("dateType") String dateType,
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate,
            @QueryParam("complete") @DefaultValue("false") boolean complete,
            @Context SecurityContext securityContext) {
        Project project = loadProject(projectId, securityContext);
        Filters f = filtersFor(1, dateType, startDate, endDate, complete, null, null, null);
        ChartSeries series = mainInventorReport.InventorDateSeries(project, clamp(limit), f);
        return toReport("main-inventor", series, false);
    }

    @GET
    @Path("/main-ipc")
    @Operation(summary = "Principais IPCs/CPCs")
    public ReportDTO mainIpc(
            @PathParam("projectId") String projectId,
            @QueryParam("limit") @DefaultValue("10") int limit,
            @QueryParam("ipcLevel") @DefaultValue("class") String ipcLevel,
            @QueryParam("classificationType") @DefaultValue("IPC") String classificationType,
            @QueryParam("dateType") String dateType,
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate,
            @QueryParam("complete") @DefaultValue("false") boolean complete,
            @Context SecurityContext securityContext) {
        Project project = loadProject(projectId, securityContext);
        boolean[] flags = parseIpcLevel(ipcLevel);
        int clsType = parseClassificationType(classificationType);
        Filters f = filtersFor(1, dateType, startDate, endDate, complete, null, null, null);
        ChartSeries series = mainIPCReport.ipcCount(
                project, flags[0], flags[1], flags[2], flags[3], clamp(limit), f, clsType);
        return toReport("main-ipc", series, !flags[0]);
    }

    @GET
    @Path("/application-date")
    @Operation(summary = "Relatório por data de depósito")
    public ReportDTO applicationDate(
            @PathParam("projectId") String projectId,
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate,
            @QueryParam("complete") @DefaultValue("false") boolean complete,
            @Context SecurityContext securityContext) {
        Project project = loadProject(projectId, securityContext);
        Filters f = filtersFor(2, "application", startDate, endDate, complete, null, null, null);
        ChartSeries series = applicationDateReport.applicationDateSeries(project, f);
        return toReport("application-date", series, false);
    }

    @GET
    @Path("/publication-date")
    @Operation(summary = "Relatório por data de publicação")
    public ReportDTO publicationDate(
            @PathParam("projectId") String projectId,
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate,
            @QueryParam("complete") @DefaultValue("false") boolean complete,
            @Context SecurityContext securityContext) {
        Project project = loadProject(projectId, securityContext);
        Filters f = filtersFor(1, "publication", startDate, endDate, complete, null, null, null);
        ChartSeries series = publicationDateReport.publicationDateSeries(project, f);
        return toReport("publication-date", series, false);
    }

    @GET
    @Path("/priority-country")
    @Operation(summary = "Países de prioridade")
    public ReportDTO priorityCountry(
            @PathParam("projectId") String projectId,
            @QueryParam("limit") @DefaultValue("10") int limit,
            @QueryParam("dateType") String dateType,
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate,
            @QueryParam("complete") @DefaultValue("false") boolean complete,
            @Context SecurityContext securityContext) {
        Project project = loadProject(projectId, securityContext);
        Filters f = filtersFor(1, dateType, startDate, endDate, complete, null, null, null);
        ChartSeries series = priorityCountryReport.mainPriorityCountrySeries(project, clamp(limit), f);
        return toReport("priority-country", series, false);
    }

    @GET
    @Path("/priority-date")
    @Operation(summary = "Prioridades por ano")
    public ReportDTO priorityDate(
            @PathParam("projectId") String projectId,
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate,
            @QueryParam("complete") @DefaultValue("false") boolean complete,
            @Context SecurityContext securityContext) {
        Project project = loadProject(projectId, securityContext);
        Filters f = filtersFor(1, null, startDate, endDate, complete, null, null, null);
        ChartSeries series = priorityDateReport.priorityDateSeries(project, f);
        if (series != null && series.getData() != null) {
            series.getData().entrySet().removeIf(e ->
                    e.getValue() != null && e.getValue().intValue() == -1
                            || "-1".equals(String.valueOf(e.getKey())));
        }
        return toReport("priority-date", series, false);
    }

    @GET
    @Path("/docs-national-priority")
    @Operation(summary = "Documentos com prioridade nacional (BR)")
    public PageDTO<PatentDTO> docsNationalPriority(
            @PathParam("projectId") String projectId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("q") String q,
            @Context SecurityContext securityContext) {
        Project project = loadProject(projectId, securityContext);
        if (page < 0) {
            page = 0;
        }
        if (size <= 0 || size > 200) {
            size = 20;
        }
        Map<String, String> filters = new HashMap<>();
        if (q != null && !q.isBlank()) {
            filters.put("titleSelect", q.trim());
        }
        PatentRepository.PatentPage result = patentRepository.loadBrazilianPage(
                project, false, page * size, size, "publicationDate", 1, filters);
        return PageDTO.of(
                result.content().stream().map(p -> PatentDTO.from(p, projectId)).toList(),
                page, size, result.total());
    }

    @GET
    @Path("/technological-profile-applicant")
    @Operation(summary = "Perfil tecnológico por depositante")
    public ReportDTO technologicalProfileApplicant(
            @PathParam("projectId") String projectId,
            @QueryParam("applicantName") String applicantName,
            @QueryParam("limit") @DefaultValue("10") int limit,
            @QueryParam("ipcLevel") @DefaultValue("section") String ipcLevel,
            @QueryParam("classificationType") @DefaultValue("IPC") String classificationType,
            @QueryParam("dateType") String dateType,
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate,
            @QueryParam("complete") @DefaultValue("false") boolean complete,
            @Context SecurityContext securityContext) {
        Project project = loadProject(projectId, securityContext);
        boolean[] flags = parseIpcLevel(ipcLevel);
        int clsType = parseClassificationType(classificationType);
        Filters f = filtersFor(1, dateType, startDate, endDate, complete, null, applicantName, null);
        ChartSeries series = mainIPCReport.ipcCount(
                project, flags[0], flags[1], flags[2], flags[3], clamp(limit), f, clsType);
        return toReport("technological-profile-applicant", series, !flags[0]);
    }

    @GET
    @Path("/technological-profile-inventor")
    @Operation(summary = "Perfil tecnológico por inventor")
    public ReportDTO technologicalProfileInventor(
            @PathParam("projectId") String projectId,
            @QueryParam("inventorName") String inventorName,
            @QueryParam("limit") @DefaultValue("10") int limit,
            @QueryParam("ipcLevel") @DefaultValue("section") String ipcLevel,
            @QueryParam("classificationType") @DefaultValue("IPC") String classificationType,
            @QueryParam("dateType") String dateType,
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate,
            @QueryParam("complete") @DefaultValue("false") boolean complete,
            @Context SecurityContext securityContext) {
        Project project = loadProject(projectId, securityContext);
        boolean[] flags = parseIpcLevel(ipcLevel);
        int clsType = parseClassificationType(classificationType);
        Filters f = filtersFor(1, dateType, startDate, endDate, complete, null, null, inventorName);
        ChartSeries series = mainIPCReport.ipcCount(
                project, flags[0], flags[1], flags[2], flags[3], clamp(limit), f, clsType);
        return toReport("technological-profile-inventor", series, !flags[0]);
    }

    @GET
    @Path("/nature-applicant")
    @Operation(summary = "Natureza dos depositantes")
    public ReportDTO natureApplicant(
            @PathParam("projectId") String projectId,
            @QueryParam("dateType") String dateType,
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate,
            @QueryParam("complete") @DefaultValue("false") boolean complete,
            @Context SecurityContext securityContext) {
        Project project = loadProject(projectId, securityContext);
        Filters f = filtersFor(1, dateType, startDate, endDate, complete, null, null, null);
        ChartSeries series = mainNatureApplicantReport.NatureApplicantSeries(project, f);
        return toReport("nature-applicant", series, false);
    }

    @GET
    @Path("/state-distribution")
    @Operation(summary = "Distribuição estadual (Brasil)")
    public ReportDTO stateDistribution(
            @PathParam("projectId") String projectId,
            @QueryParam("dateType") String dateType,
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate,
            @QueryParam("complete") @DefaultValue("false") boolean complete,
            @Context SecurityContext securityContext) {
        Project project = loadProject(projectId, securityContext);
        Filters f = filtersFor(1, dateType, startDate, endDate, complete, null, null, null);
        ChartSeries series = stateDistribuitionReport.StateDistribuitionSeries(project, f);
        return toReport("state-distribution", series, false);
    }

    @GET
    @Path("/region-distribution")
    @Operation(summary = "Distribuição regional (Brasil)")
    public ReportDTO regionDistribution(
            @PathParam("projectId") String projectId,
            @QueryParam("dateType") String dateType,
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate,
            @QueryParam("complete") @DefaultValue("false") boolean complete,
            @Context SecurityContext securityContext) {
        Project project = loadProject(projectId, securityContext);
        Filters f = filtersFor(1, dateType, startDate, endDate, complete, null, null, null);
        ChartSeries series = regionDistribuitionReport.RegionDistribuitionSeries(project, f);
        return toReport("region-distribution", series, false);
    }

    @GET
    @Path("/applicants")
    @Operation(summary = "Autocomplete de depositantes do projeto")
    public List<String> listApplicants(
            @PathParam("projectId") String projectId,
            @QueryParam("q") @DefaultValue("") String q,
            @Context SecurityContext securityContext) {
        Project project = loadProject(projectId, securityContext);
        return applicantRepository.getApplicants(project, q != null ? q.trim() : "");
    }

    @GET
    @Path("/inventors")
    @Operation(summary = "Autocomplete de inventores do projeto")
    public List<String> listInventors(
            @PathParam("projectId") String projectId,
            @QueryParam("q") @DefaultValue("") String q,
            @Context SecurityContext securityContext) {
        Project project = loadProject(projectId, securityContext);
        return inventorRepository.getInventors(project, q != null ? q.trim() : "");
    }

    private Project loadProject(String projectId, SecurityContext securityContext) {
        User user = securitySupport.requireUser(securityContext);
        return securitySupport.requireProject(projectId, user);
    }

    static Filters filtersFor(
            int defaultDateType,
            String dateType,
            String startDate,
            String endDate,
            boolean complete,
            String applicantType,
            String applicantName,
            String inventorName) {
        Filters f = new Filters();
        f.setComplete(complete);
        int selecionaData = defaultDateType;
        if ("application".equalsIgnoreCase(dateType)) {
            selecionaData = 2;
        } else if ("publication".equalsIgnoreCase(dateType)) {
            selecionaData = 1;
        }
        f.setSelecionaData(selecionaData);

        boolean hasStart = startDate != null && !startDate.isBlank();
        boolean hasEnd = endDate != null && !endDate.isBlank();
        if (hasStart || hasEnd) {
            f.setInicio(hasStart ? parseFilterDate(startDate, false) : new Date(0L));
            f.setFim(hasEnd ? parseFilterDate(endDate, true) : new Date());
        }
        if (applicantType != null && !applicantType.isBlank()) {
            f.setApplicantType(applicantType.trim());
        }
        if (applicantName != null && !applicantName.isBlank()) {
            f.setApplicantName(applicantName.trim());
        }
        if (inventorName != null && !inventorName.isBlank()) {
            f.setInventorName(inventorName.trim());
        }
        return f;
    }

    private static Date parseFilterDate(String raw, boolean endOfDay) {
        String trimmed = raw.trim();
        try {
            LocalDate ld = LocalDate.parse(trimmed);
            if (endOfDay) {
                return Date.from(ld.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusMillis(1));
            }
            return Date.from(ld.atStartOfDay(ZoneOffset.UTC).toInstant());
        } catch (DateTimeParseException ignored) {
            return PatentMapperSupport.parseDate(trimmed);
        }
    }

    static boolean[] parseIpcLevel(String ipcLevel) {
        if (ipcLevel == null || ipcLevel.isBlank()) {
            // Default: class (klass=true, subKlass=false, group=false, subGroup=false)
            return new boolean[]{true, false, false, false};
        }
        return switch (ipcLevel.trim().toLowerCase()) {
            case "section" -> new boolean[]{false, false, false, false};
            case "class" -> new boolean[]{true, false, false, false};
            case "subclass" -> new boolean[]{true, true, false, false};
            case "group" -> new boolean[]{true, true, true, false};
            case "subgroup" -> new boolean[]{true, true, true, true};
            default -> throw ApiException.badRequest(
                    "ipcLevel must be section, class, subclass, group or subgroup");
        };
    }

    static int parseClassificationType(String classificationType) {
        if (classificationType == null || classificationType.isBlank() || "IPC".equalsIgnoreCase(classificationType.trim())) {
            return 1;
        }
        if ("CPC".equalsIgnoreCase(classificationType.trim())) {
            return 2;
        }
        throw ApiException.badRequest("classificationType must be IPC or CPC");
    }

    private static int clamp(int limit) {
        if (limit <= 0) {
            return 10;
        }
        return Math.min(limit, 100);
    }

    private static ReportDTO toReport(String name, ChartSeries series, boolean includeSectionDescription) {
        List<ReportItemDTO> items = new ArrayList<>();
        if (series != null && series.getData() != null) {
            for (Map.Entry<Object, Number> e : series.getData().entrySet()) {
                String key = String.valueOf(e.getKey());
                String desc = null;
                if (includeSectionDescription && key.length() == 1) {
                    desc = IPC_SECTION_DESCRIPTIONS.get(Character.toUpperCase(key.charAt(0)));
                }
                items.add(new ReportItemDTO(key, e.getValue(), desc));
            }
        }
        String label = series != null ? series.getLabel() : name;
        return new ReportDTO(name, label, items);
    }
}
