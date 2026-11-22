package br.ufmt.periscope.report;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.repository.StateDistribuitionRepository;
import br.ufmt.periscope.util.Filters;
import java.util.Collections;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

@ApplicationScoped
@Named
public class StateDistribuitionReport {

    private @Inject
    StateDistribuitionRepository repo;

    public ChartSeries StateDistribuitionSeries(Project currentProject) {
        return StateDistribuitionSeries(currentProject, null);
    }

    public ChartSeries StateDistribuitionSeries(Project currentProject, Filters filtro) {
        ChartSeries series = new ChartSeries("Distribuição por Estados");
        List<Pair> i = repo.getStateDistribuitions(currentProject, filtro);
        Collections.reverse(i);

        for (Pair pair : i) {
            String state = (String) pair.getKey();
            Integer count = (Integer) pair.getValue();
            series.set(state, count);
        }

        return series;
    }

}