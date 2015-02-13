package br.ufmt.periscope.report;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.repository.PriorityDateRepository;
import br.ufmt.periscope.util.Filters;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.model.chart.ChartSeries;

@Named
public class PriorityDateReport {

    private @Inject
    PriorityDateRepository repo;

    public ChartSeries priorityDateSeries(Project currentProject, Filters filtro) {
        ChartSeries series = new ChartSeries("Prioridades por ano");

        List<Pair> i = repo.getPrioritiesByDate(currentProject, filtro);

        Collections.reverse(i);

        for (Pair pair : i) {
            Integer year = Integer.parseInt((String) pair.getKey());
            Integer count = (Integer) pair.getValue();
            series.set(year, count);
        }

        return series;

    }
}