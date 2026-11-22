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
import dev.morphia.query.Query;
import jakarta.ws.rs.core.Response;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectResourceTest {

    @Mock
    private Datastore ds;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ApiSecuritySupport securitySupport;

    @InjectMocks
    private ProjectResource projectResource;

    private User owner;
    private User observer;
    private SecurityContext secCtx;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(new ObjectId());
        owner.setUsername("owner");
        owner.setUserLevel(UserLevel.USER);

        observer = new User();
        observer.setId(new ObjectId());
        observer.setUsername("obs");
        observer.setUserLevel(UserLevel.USER);

        secCtx = mock(SecurityContext.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void createResolvesObserversExcludingOwnerAndDuplicates() {
        when(securitySupport.requireUser(secCtx)).thenReturn(owner);
        Query<User> query = mock(Query.class);
        when(ds.find(User.class)).thenReturn(query);
        when(query.filter(any())).thenReturn(query);
        when(query.first()).thenReturn(observer);

        ProjectRequest req = new ProjectRequest(
                "Shared Project",
                "desc",
                false,
                List.of(owner.getId().toString(), observer.getId().toString(), observer.getId().toString()));

        Response resp = projectResource.create(req, secCtx);
        assertThat(resp.getStatus()).isEqualTo(201);
        ProjectDTO dto = (ProjectDTO) resp.getEntity();
        assertThat(dto.observerIds()).containsExactly(observer.getId().toString());
    }

    @Test
    void nonOwnerNonAdminCannotModifyObserversOrUpdateProject() {
        Project project = new Project();
        project.setId(new ObjectId());
        project.setTitle("P1");
        project.setOwner(owner);

        when(securitySupport.requireUser(secCtx)).thenReturn(observer);
        when(securitySupport.requireProject(project.getId().toString(), observer)).thenReturn(project);

        assertThatThrownBy(() -> projectResource.update(
                project.getId().toString(),
                new ProjectRequest("Hacked", null, null, null),
                secCtx))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(403));

        assertThatThrownBy(() -> projectResource.addObserver(
                project.getId().toString(), observer.getId().toString(), secCtx))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(403));
    }

    @Test
    @SuppressWarnings("unchecked")
    void ownerCanAddAndRemoveObserver() {
        Project project = new Project();
        project.setId(new ObjectId());
        project.setTitle("P1");
        project.setOwner(owner);

        when(securitySupport.requireUser(secCtx)).thenReturn(owner);
        when(securitySupport.requireProject(project.getId().toString(), owner)).thenReturn(project);
        when(securitySupport.countPatents(project)).thenReturn(0L);

        Query<User> query = mock(Query.class);
        when(ds.find(User.class)).thenReturn(query);
        when(query.filter(any())).thenReturn(query);
        when(query.first()).thenReturn(observer);

        ProjectDTO added = projectResource.addObserver(
                project.getId().toString(), observer.getId().toString(), secCtx);
        assertThat(added.observerIds()).containsExactly(observer.getId().toString());

        ProjectDTO removed = projectResource.removeObserver(
                project.getId().toString(), observer.getId().toString(), secCtx);
        assertThat(removed.observerIds()).isEmpty();
        verify(ds, org.mockito.Mockito.times(2)).save(project);
    }
}
