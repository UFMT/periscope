package br.ufmt.periscope.api.dto;

import br.ufmt.periscope.enumerated.ClassificationType;
import br.ufmt.periscope.model.Classification;
import br.ufmt.periscope.model.Country;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Priority;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.Rule;
import br.ufmt.periscope.model.RuleType;
import br.ufmt.periscope.model.State;
import br.ufmt.periscope.model.User;
import br.ufmt.periscope.model.UserLevel;
import java.util.Date;
import java.util.List;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DtoMappingTest {

    @Test
    void userDtoNeverExposesPasswordAndMapsMustChangePassword() {
        User user = new User();
        user.setId(new ObjectId());
        user.setUsername("admin");
        user.setPassword("secret-or-hash");
        user.setFirstname("Admin");
        user.setLastname("User");
        user.setEmail("admin@example.com");
        user.setUserLevel(UserLevel.ADMIN);
        user.setMustChangePassword(true);

        UserDTO dto = UserDTO.from(user);

        assertThat(dto.id()).isEqualTo(user.getId().toString());
        assertThat(dto.username()).isEqualTo("admin");
        assertThat(dto.firstname()).isEqualTo("Admin");
        assertThat(dto.userLevel()).isEqualTo("ADMIN");
        assertThat(dto.mustChangePassword()).isTrue();
        assertThat(dto.toString()).doesNotContain("secret");
        assertThat(dto.getClass().getRecordComponents())
                .extracting(rc -> rc.getName())
                .doesNotContain("password");
    }

    @Test
    void projectDtoMapsOwnerObserversAndCounts() {
        User owner = new User();
        owner.setId(new ObjectId());
        owner.setFirstname("Ada");
        owner.setLastname("Lovelace");

        User observer = new User();
        observer.setId(new ObjectId());
        observer.setUsername("bob");
        observer.setFirstname("Bob");

        Project project = new Project();
        project.setId(new ObjectId());
        project.setTitle("Solar");
        project.setDescription("desc");
        project.setIsPublic(true);
        project.setCreatedAt(new Date());
        project.setUpdateAt(new Date());
        project.setOwner(owner);
        project.setObservers(List.of(observer));

        ProjectDTO dto = ProjectDTO.from(project, 42);

        assertThat(dto.id()).isEqualTo(project.getId().toString());
        assertThat(dto.title()).isEqualTo("Solar");
        assertThat(dto.ownerId()).isEqualTo(owner.getId().toString());
        assertThat(dto.ownerName()).contains("Ada");
        assertThat(dto.patentCount()).isEqualTo(42);
        assertThat(dto.isPublic()).isTrue();
        assertThat(dto.observerIds()).containsExactly(observer.getId().toString());
        assertThat(dto.observers()).extracting(UserSummaryDTO::username).containsExactly("bob");
    }

    @Test
    void patentDtoFromWithExplicitProjectIdDoesNotNeedProjectReference() {
        Patent patent = new Patent();
        patent.setId(new ObjectId());
        patent.setTitleSelect("Solar Tracking");
        patent.setPublicationNumber("DE1");
        patent.setLanguage("EN");
        patent.setClassifications(List.of(new Classification("H01L 31/04", ClassificationType.ICAI)));
        Priority pr = new Priority();
        pr.setValue("BR102020");
        Country br = new Country();
        br.setAcronym("BR");
        pr.setCountry(br);
        pr.setDate(new Date());
        patent.setPriorities(List.of(pr));
        // deliberately leave project null — list path must not dereference it
        patent.setProject(null);

        String projectId = new ObjectId().toString();
        PatentDTO dto = PatentDTO.from(patent, projectId);

        assertThat(dto.projectId()).isEqualTo(projectId);
        assertThat(dto.title()).isEqualTo("SOLAR TRACKING");
        assertThat(dto.id()).isEqualTo(patent.getId().toString());
        assertThat(dto.language()).isEqualTo("EN");
        assertThat(dto.classifications()).containsExactly("H01L 31/04");
        assertThat(dto.priorities()).hasSize(1);
        assertThat(dto.priorities().get(0).countryAcronym()).isEqualTo("BR");
    }

    @Test
    void ruleDtoFromWithExplicitProjectIdMapsStateAndSkipsProjectRef() {
        Rule rule = new Rule();
        rule.setId(new ObjectId());
        rule.setName("ACME GmbH");
        rule.setType(RuleType.APPLICANT);
        State state = new State();
        state.setAcronym("MT");
        rule.setState(state);
        // deliberately leave project null — list projections omit it
        rule.setProject(null);

        String projectId = new ObjectId().toString();
        RuleDTO dto = RuleDTO.from(rule, projectId);

        assertThat(dto.projectId()).isEqualTo(projectId);
        assertThat(dto.name()).isEqualTo("ACME GMBH");
        assertThat(dto.type()).isEqualTo("APPLICANT");
        assertThat(dto.stateAcronym()).isEqualTo("MT");
        assertThat(dto.id()).isEqualTo(rule.getId().toString());
    }
}
