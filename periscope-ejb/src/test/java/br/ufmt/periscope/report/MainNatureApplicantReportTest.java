package br.ufmt.periscope.report;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.repository.NatureApplicantRepository;
import br.ufmt.periscope.util.Filters;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MainNatureApplicantReportTest {

    @Mock
    private NatureApplicantRepository repo;

    @InjectMocks
    private MainNatureApplicantReport report;

    @Test
    void producesRawNatureKeysWithoutFacesContext() {
        Project project = new Project();
        Filters filters = new Filters();
        when(repo.getNatureApplicantRepository(project, filters))
                .thenReturn(Arrays.asList(
                        new Pair("private.company", 5),
                        new Pair("public.university", 2)));

        ChartSeries series = report.NatureApplicantSeries(project, filters);

        assertThat(series.getLabel()).isEqualTo("Natureza dos Depositantes");
        assertThat(series.getData()).containsEntry("private.company", 5)
                .containsEntry("public.university", 2);
    }
}
