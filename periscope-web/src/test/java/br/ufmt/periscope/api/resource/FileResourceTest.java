package br.ufmt.periscope.api.resource;

import br.ufmt.periscope.api.ApiException;
import br.ufmt.periscope.api.security.ApiSecuritySupport;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.User;
import br.ufmt.periscope.model.UserLevel;
import br.ufmt.periscope.repository.PatentRepository;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import dev.morphia.Datastore;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.bson.Document;
import org.bson.conversions.Bson;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileResourceTest {

    @Mock
    private GridFSBucket gridFSBucket;

    @Mock
    private ApiSecuritySupport securitySupport;

    @Mock
    private PatentRepository patentRepository;

    @Mock
    private Datastore ds;

    @InjectMocks
    private FileResource resource;

    private SecurityContext secCtx;
    private User normalUser;
    private User adminUser;
    private ObjectId fileId;
    private String projectId;

    @BeforeEach
    void setUp() {
        secCtx = mock(SecurityContext.class);
        normalUser = new User();
        normalUser.setId(new ObjectId());
        normalUser.setUsername("bob");
        normalUser.setUserLevel(UserLevel.USER);

        adminUser = new User();
        adminUser.setId(new ObjectId());
        adminUser.setUsername("admin");
        adminUser.setUserLevel(UserLevel.ADMIN);

        fileId = new ObjectId();
        projectId = new ObjectId().toString();
    }

    @Test
    void blocksDownloadWhenUserLacksProjectAccess() {
        when(securitySupport.requireUser(secCtx)).thenReturn(normalUser);
        when(patentRepository.findProjectIdByFileId(fileId)).thenReturn(projectId);
        when(securitySupport.requireProject(projectId, normalUser))
                .thenThrow(ApiException.forbidden("No access to this project"));

        assertThatThrownBy(() -> resource.download(fileId.toString(), secCtx))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(403));
        verifyNoInteractions(gridFSBucket);
    }

    @Test
    void hidesUnlinkedFilesFromNonAdmins() {
        when(securitySupport.requireUser(secCtx)).thenReturn(normalUser);
        when(patentRepository.findProjectIdByFileId(fileId)).thenReturn(null);

        assertThatThrownBy(() -> resource.download(fileId.toString(), secCtx))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(404));
        verifyNoInteractions(gridFSBucket);
    }

    @Test
    void allowsAuthorizedDownloadAndSanitizesFilename() {
        when(securitySupport.requireUser(secCtx)).thenReturn(normalUser);
        when(patentRepository.findProjectIdByFileId(fileId)).thenReturn(projectId);
        when(securitySupport.requireProject(projectId, normalUser)).thenReturn(new Project());

        GridFSFindIterable iterable = mock(GridFSFindIterable.class);
        GridFSFile gfile = mock(GridFSFile.class);
        when(gridFSBucket.find(any(Bson.class))).thenReturn(iterable);
        when(iterable.first()).thenReturn(gfile);
        when(gfile.getFilename()).thenReturn("doc\"\r\n.pdf");
        when(gfile.getMetadata()).thenReturn(new Document("contentType", "application/pdf"));

        Response response = resource.download(fileId.toString(), secCtx);

        assertThat(response.getStatus()).isEqualTo(200);
        String disp = response.getHeaderString("Content-Disposition");
        assertThat(disp).contains("attachment; filename=\"doc___.pdf\"");
        assertThat(disp).doesNotContain("\r").doesNotContain("\n");
    }

    @Test
    void allowsAdminToDownloadUnlinkedFile() {
        when(securitySupport.requireUser(secCtx)).thenReturn(adminUser);
        when(patentRepository.findProjectIdByFileId(fileId)).thenReturn(null);

        GridFSFindIterable iterable = mock(GridFSFindIterable.class);
        GridFSFile gfile = mock(GridFSFile.class);
        when(gridFSBucket.find(any(Bson.class))).thenReturn(iterable);
        when(iterable.first()).thenReturn(gfile);
        when(gfile.getFilename()).thenReturn("raw.bin");

        Response response = resource.download(fileId.toString(), secCtx);
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
