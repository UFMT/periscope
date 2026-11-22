package br.ufmt.periscope.report;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.repository.NatureApplicantRepository;
import br.ufmt.periscope.util.Filters;
import java.util.Collections;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

@ApplicationScoped
@Named
public class MainNatureApplicantReport {

    private @Inject
    NatureApplicantRepository repo;

    public ChartSeries NatureApplicantSeries(Project currentProject, Filters filtro) {
        ChartSeries series = new ChartSeries("Natureza dos Depositantes");

        List<Pair> i = repo.getNatureApplicantRepository(currentProject, filtro);

        Collections.reverse(i);

        for (Pair pair : i) {
            if (pair.getKey() == null) {
                continue;
            }
            String nature = String.valueOf(pair.getKey());
            Integer count = pair.getValue() instanceof Number n ? n.intValue() : 0;
            series.set(nature, count);
        }

        return series;
    }

}