package br.ufmt.periscope.report;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.model.chart.ChartSeries;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.repository.ApplicationDateRepository;
import br.ufmt.periscope.util.Filters;
import java.util.Collections;

@Named
public class ApplicationDateReport {

    private @Inject
    ApplicationDateRepository repo;

    public ChartSeries applicationDateSeries(Project currentProject, Filters filtro) {
        ChartSeries series = new ChartSeries("Depositos por ano");

        List<Pair> i = repo.getApplicationsByDate(currentProject, filtro);

        Collections.reverse(i);

        for (Pair pair : i) {
            Integer year = Integer.parseInt((String) pair.getKey());
            Integer count = (Integer) pair.getValue();
            series.set(year, count);
        }

        return series;
    }

}
