package br.ufmt.periscope.api.resource;

import br.ufmt.periscope.api.dto.HarmonizationEntityDTO;
import br.ufmt.periscope.api.dto.PageDTO;
import br.ufmt.periscope.api.dto.RuleDTO;
import br.ufmt.periscope.api.dto.RuleRequest;
import br.ufmt.periscope.api.security.ApiSecuritySupport;
import br.ufmt.periscope.harmonization.Harmonization;
import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.model.ApplicantType;
import br.ufmt.periscope.model.Country;
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
import jakarta.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HarmonizationResourceTest {

    @Mock
    private ApiSecuritySupport securitySupport;

    @Mock
    private ApplicantRepository applicantRepository;

    @Mock
    private InventorRepository inventorRepository;

    @Mock
    private RuleRepository ruleRepository;

    @Mock
    private Harmonization harmonization;

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private ApplicantTypeRepository applicantTypeRepository;

    @InjectMocks
    private HarmonizationResource resource;

    private User user;
    private Project project;
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

    @Test
    void updatesRuleWithCountryStateAndNature() {
        ObjectId ruleId = new ObjectId();
        Rule existing = new Rule();
        existing.setId(ruleId);
        existing.setProject(project);
        existing.setType(RuleType.APPLICANT);
        existing.setName("OLD");

        Country br = new Country();
        br.setAcronym("BR");
        br.setName("BRAZIL");
        State mt = new State();
        mt.setAcronym("MT");
        mt.setName("Mato Grosso");
        mt.setRegion("Centro-Oeste");
        br.setStates(List.of(mt));

        when(ruleRepository.findById(ruleId.toString())).thenReturn(existing);
        when(countryRepository.getCountryByAcronym("BR")).thenReturn(br);
        when(applicantTypeRepository.getAll()).thenReturn(List.of(new ApplicantType("public.university")));

        RuleRequest req = new RuleRequest(
                "UFMT", "UFMT", "APPLICANT", Set.of("UNIV FED MATO GROSSO"),
                "BR", "MT", "public.university");

        RuleDTO updated = resource.updateRule(project.getId().toString(), ruleId.toString(), req, secCtx);

        verify(ruleRepository).update(existing);
        assertThat(updated.name()).isEqualTo("UFMT");
        assertThat(updated.countryAcronym()).isEqualTo("BR");
        assertThat(updated.stateAcronym()).isEqualTo("MT");
        assertThat(updated.nature()).isEqualTo("public.university");
    }

    @Test
    void undoRuleDispatchesByRuleType() {
        ObjectId ruleId = new ObjectId();
        Rule rule = new Rule();
        rule.setId(ruleId);
        rule.setProject(project);
        rule.setName("ACME");
        rule.setType(RuleType.APPLICANT);
        when(ruleRepository.findById(ruleId.toString())).thenReturn(rule);

        Map<String, Object> result = resource.undoRule(project.getId().toString(), ruleId.toString(), secCtx);

        verify(ruleRepository).undoApplicantRule(project, "ACME");
        assertThat(result).containsEntry("undone", 1);
    }

    @Test
    void listEntitiesReturnsPagedApplicants() {
        Applicant app = new Applicant("ACME");
        app.setHarmonized(true);
        app.setDocumentCount(4);
        when(applicantRepository.load(eq(0), eq(10), eq("documentCount"), eq(1), any()))
                .thenReturn(List.of(app));
        when(applicantRepository.getCount()).thenReturn(1);

        PageDTO<HarmonizationEntityDTO> page = resource.listEntities(
                project.getId().toString(), "applicant", 0, 10, "ACME", secCtx);

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content().get(0).name()).isEqualTo("ACME");
        assertThat(page.content().get(0).documentCount()).isEqualTo(4);
    }
}
