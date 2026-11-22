package br.ufmt.periscope.api.resource;

import br.ufmt.periscope.api.ApiException;
import br.ufmt.periscope.api.dto.PatentCreateRequest;
import br.ufmt.periscope.api.dto.PatentDTO;
import br.ufmt.periscope.api.dto.PatentUpdateRequest;
import br.ufmt.periscope.api.dto.PriorityInputDTO;
import br.ufmt.periscope.api.security.ApiSecuritySupport;
import br.ufmt.periscope.model.Country;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.User;
import br.ufmt.periscope.repository.CountryRepository;
import br.ufmt.periscope.repository.PatentRepository;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import dev.morphia.Datastore;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.List;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatentResourceTest {

    @Mock
    private Datastore ds;

    @Mock
    private PatentRepository patentRepository;

    @Mock
    private ApiSecuritySupport securitySupport;

    @Mock
    private CountryRepository countryRepository;

    private PatentMapperSupport patentMapper;
    private ProjectPatentResource projectPatentResource;
    private PatentResource patentResource;

    private User user;
    private Project project;
    private SecurityContext secCtx;

    @BeforeEach
    void setUp() throws Exception {
        patentMapper = new PatentMapperSupport(countryRepository);
        projectPatentResource = new ProjectPatentResource();
        patentResource = new PatentResource();

        inject(projectPatentResource, "ds", ds);
        inject(projectPatentResource, "patentRepository", patentRepository);
        inject(projectPatentResource, "securitySupport", securitySupport);
        inject(projectPatentResource, "patentMapper", patentMapper);

        inject(patentResource, "ds", ds);
        inject(patentResource, "patentRepository", patentRepository);
        inject(patentResource, "securitySupport", securitySupport);
        inject(patentResource, "patentMapper", patentMapper);

        user = new User();
        user.setId(new ObjectId());
        user.setUsername("alice");

        project = new Project();
        project.setId(new ObjectId());
        project.setTitle("Proj");

        secCtx = mock(SecurityContext.class);
    }

    @Test
    void createValidatesRejectsConflictAndSavesPatent() {
        when(securitySupport.requireUser(secCtx)).thenReturn(user);
        when(securitySupport.requireProject(project.getId().toString(), user)).thenReturn(project);

        Country br = new Country();
        br.setAcronym("BR");
        br.setName("BRAZIL");
        when(countryRepository.getCountryByAcronym("BR")).thenReturn(br);

        PatentCreateRequest missingNum = new PatentCreateRequest(
                "Title", null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        assertThatThrownBy(() -> projectPatentResource.create(project.getId().toString(), missingNum, secCtx))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(400));

        PatentCreateRequest valid = new PatentCreateRequest(
                "Solar Cell", "Abstract", "BR100", "2024-02-10", "APP100", "2023-01-05",
                "BR", "PT", "H01L 31/04", List.of("H02S 40/00"),
                List.of("UFMT"), List.of("Ada"),
                List.of(new PriorityInputDTO("BRPR1", "BR", "2022-12-01")),
                false, true);

        when(patentRepository.patentExistsForProject(any(Patent.class), any(Project.class)))
                .thenReturn(true);
        assertThatThrownBy(() -> projectPatentResource.create(project.getId().toString(), valid, secCtx))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(409));

        when(patentRepository.patentExistsForProject(any(Patent.class), any(Project.class)))
                .thenReturn(false);

        Response resp = projectPatentResource.create(project.getId().toString(), valid, secCtx);
        assertThat(resp.getStatus()).isEqualTo(201);
        PatentDTO dto = (PatentDTO) resp.getEntity();
        assertThat(dto.title()).isEqualTo("SOLAR CELL");
        assertThat(dto.applicationCountry()).isEqualTo("BR");
        assertThat(dto.applicants()).containsExactly("UFMT");
        assertThat(dto.inventors()).containsExactly("ADA");
        assertThat(dto.priorities()).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void updateAppliesFullFieldsAndPreservesExplicitCompleted() {
        ObjectId patentId = new ObjectId();
        Patent existing = new Patent();
        existing.setId(patentId);
        existing.setTitleSelect("OLD TITLE");

        Mapper mapper = mock(Mapper.class);
        EntityModel model = mock(EntityModel.class);
        MongoDatabase db = mock(MongoDatabase.class);
        MongoCollection<Document> coll = mock(MongoCollection.class);
        FindIterable<Document> iterable = mock(FindIterable.class);
        when(ds.getMapper()).thenReturn(mapper);
        when(mapper.getEntityModel(Patent.class)).thenReturn(model);
        when(model.getCollectionName()).thenReturn("Patent");
        when(ds.getDatabase()).thenReturn(db);
        when(db.getCollection("Patent")).thenReturn(coll);
        when(coll.find(any(Bson.class))).thenReturn(iterable);
        when(iterable.projection(any(Bson.class))).thenReturn(iterable);
        when(iterable.first()).thenReturn(new Document("project", project.getId()));

        Query<Patent> query = mock(Query.class);
        when(ds.find(Patent.class)).thenReturn(query);
        when(query.filter(any())).thenReturn(query);
        when(query.first(any(FindOptions.class))).thenReturn(existing);

        when(securitySupport.requireUser(secCtx)).thenReturn(user);
        when(securitySupport.requireProject(project.getId().toString(), user)).thenReturn(project);
        org.mockito.Mockito.doAnswer(inv -> {
            ((Patent) inv.getArgument(0)).setCompleted(false);
            return null;
        }).when(patentRepository).savePatent(existing);

        PatentUpdateRequest req = new PatentUpdateRequest(
                "Updated Title", "New abstract", "DE99", "2024-05-01",
                "DEAPP99", "2023-05-01", "DE", "EN",
                "G06F 17/30", null, List.of("ACME GMBH"), List.of("John"),
                List.of(new PriorityInputDTO("DE1", "DE", "2022-05-01")),
                false, true);

        PatentDTO dto = patentResource.update(patentId.toString(), req, secCtx);

        verify(patentRepository).savePatent(existing);
        verify(ds).save(existing);
        assertThat(dto.title()).isEqualTo("UPDATED TITLE");
        assertThat(dto.completed()).isTrue();
        assertThat(dto.applicants()).containsExactly("ACME GMBH");
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        var f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
