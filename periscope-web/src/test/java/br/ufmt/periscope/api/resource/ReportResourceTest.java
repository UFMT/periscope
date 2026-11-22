package br.ufmt.periscope.api.resource;

import br.ufmt.periscope.api.dto.PageDTO;
import br.ufmt.periscope.api.dto.PatentDTO;
import br.ufmt.periscope.api.dto.ReportDTO;
import br.ufmt.periscope.api.security.ApiSecuritySupport;
import br.ufmt.periscope.model.Patent;
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
import jakarta.ws.rs.core.SecurityContext;
import java.util.List;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportResourceTest {

    @Mock private ApiSecuritySupport securitySupport;
    @Mock private PatentRepository patentRepository;
    @Mock private ApplicantRepository applicantRepository;
    @Mock private InventorRepository inventorRepository;
    @Mock private MainApplicantReport mainApplicantReport;
    @Mock private MainInventorReport mainInventorReport;
    @Mock private MainIPCReport mainIPCReport;
    @Mock private ApplicationDateReport applicationDateReport;
    @Mock private PublicationDateReport publicationDateReport;
    @Mock private PriorityCountryReport priorityCountryReport;
    @Mock private PriorityDateReport priorityDateReport;
    @Mock private MainNatureApplicantReport mainNatureApplicantReport;
    @Mock private StateDistribuitionReport stateDistribuitionReport;
    @Mock private RegionDistribuitionReport regionDistribuitionReport;

    @InjectMocks
    private ReportResource resource;

    private Project project;
    private User user;
    private SecurityContext secCtx;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(new ObjectId());
        project = new Project();
        project.setId(new ObjectId());
        secCtx = mock(SecurityContext.class);

        when(securitySupport.requireUser(secCtx)).thenReturn(user);
        when(securitySupport.requireProject(project.getId().toString(), user)).thenReturn(project);
    }

    private static ChartSeries series(String label, String key, Number value) {
        ChartSeries cs = new ChartSeries(label);
        cs.set(key, value);
        return cs;
    }

    @Test
    void servesAllTwelveSeriesReportsAndSectionDescriptions() {
        String pid = project.getId().toString();
        when(mainApplicantReport.mainApplicantSeries(eq(project), eq(10), any(Filters.class)))
                .thenReturn(series("Applicants", "ACME", 3));
        when(mainInventorReport.InventorDateSeries(eq(project), eq(10), any(Filters.class)))
                .thenReturn(series("Inventors", "ADA", 2));
        when(mainIPCReport.ipcCount(eq(project), eq(false), eq(false), eq(false), eq(false),
                eq(10), any(Filters.class), eq(1)))
                .thenReturn(series("IPC", "H", 5));
        when(applicationDateReport.applicationDateSeries(eq(project), any(Filters.class)))
                .thenReturn(series("AppDate", "2024", 4));
        when(publicationDateReport.publicationDateSeries(eq(project), any(Filters.class)))
                .thenReturn(series("PubDate", "2024", 4));
        when(priorityCountryReport.mainPriorityCountrySeries(eq(project), eq(10), any(Filters.class)))
                .thenReturn(series("PriorityCountry", "BR", 6));

        ChartSeries priDateSeries = new ChartSeries("PriorityDate");
        priDateSeries.set(2022, 3);
        priDateSeries.set(-1, 99);
        when(priorityDateReport.priorityDateSeries(eq(project), any(Filters.class)))
                .thenReturn(priDateSeries);

        when(mainNatureApplicantReport.NatureApplicantSeries(eq(project), any(Filters.class)))
                .thenReturn(series("Nature", "private.company", 7));
        when(stateDistribuitionReport.StateDistribuitionSeries(eq(project), any(Filters.class)))
                .thenReturn(series("State", "MT", 2));
        when(regionDistribuitionReport.RegionDistribuitionSeries(eq(project), any(Filters.class)))
                .thenReturn(series("Region", "Centro-Oeste", 2));

        assertThat(resource.mainApplicant(pid, 10, null, null, null, false, null, secCtx)
                .items().get(0).key()).isEqualTo("ACME");
        assertThat(resource.mainInventor(pid, 10, null, null, null, false, secCtx)
                .items().get(0).key()).isEqualTo("ADA");

        ReportDTO ipcSection = resource.mainIpc(
                pid, 10, "section", "IPC", null, "2020-01-01", "2024-12-31", true, secCtx);
        assertThat(ipcSection.items().get(0).description()).isEqualTo("Eletricidade");

        assertThat(resource.applicationDate(pid, null, null, false, secCtx).name()).isEqualTo("application-date");
        assertThat(resource.publicationDate(pid, null, null, false, secCtx).name()).isEqualTo("publication-date");
        assertThat(resource.priorityCountry(pid, 10, null, null, null, false, secCtx).items().get(0).key()).isEqualTo("BR");

        ReportDTO priDate = resource.priorityDate(pid, null, null, false, secCtx);
        assertThat(priDate.items()).hasSize(1);
        assertThat(priDate.items().get(0).key()).isEqualTo("2022");

        when(mainIPCReport.ipcCount(eq(project), eq(false), eq(false), eq(false), eq(false),
                anyInt(), any(Filters.class), eq(2)))
                .thenReturn(series("CPC", "A", 1));
        assertThat(resource.technologicalProfileApplicant(
                pid, "ACME", 10, "section", "CPC", null, null, null, false, secCtx).items().get(0).description())
                .isEqualTo("Necessidades Humanas");
        assertThat(resource.technologicalProfileInventor(
                pid, "ADA", 10, "section", "CPC", null, null, null, false, secCtx).items().get(0).key())
                .isEqualTo("A");

        assertThat(resource.natureApplicant(pid, null, null, null, false, secCtx).items().get(0).key())
                .isEqualTo("private.company");
        assertThat(resource.stateDistribution(pid, null, null, null, false, secCtx).items().get(0).key())
                .isEqualTo("MT");
        assertThat(resource.regionDistribution(pid, null, null, null, false, secCtx).items().get(0).key())
                .isEqualTo("Centro-Oeste");
    }

    @Test
    void servesNationalPriorityPagedDocsAndAutocomplete() {
        String pid = project.getId().toString();
        Patent p = new Patent();
        p.setId(new ObjectId());
        p.setTitleSelect("BR PATENT");
        when(patentRepository.loadBrazilianPage(eq(project), eq(false), eq(0), eq(20), eq("publicationDate"), eq(1), any()))
                .thenReturn(new PatentRepository.PatentPage(List.of(p), 1L));
        when(applicantRepository.getApplicants(project, "AC")).thenReturn(List.of("ACME"));
        when(inventorRepository.getInventors(project, "AD")).thenReturn(List.of("ADA"));

        PageDTO<PatentDTO> docs = resource.docsNationalPriority(pid, 0, 20, "BR", secCtx);
        assertThat(docs.totalElements()).isEqualTo(1L);
        assertThat(docs.content().get(0).title()).isEqualTo("BR PATENT");
        assertThat(resource.listApplicants(pid, "AC", secCtx)).containsExactly("ACME");
        assertThat(resource.listInventors(pid, "AD", secCtx)).containsExactly("ADA");
    }
}
