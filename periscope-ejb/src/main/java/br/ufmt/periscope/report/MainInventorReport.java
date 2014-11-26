package br.ufmt.periscope.report;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.model.chart.ChartSeries;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.repository.InventorRepository;
import br.ufmt.periscope.util.Filters;

@Named
public class MainInventorReport {

    private @Inject
    InventorRepository repo;

    public ChartSeries InventorDateSeries(Project currentProject, int limit, Filters filtro) {
        ChartSeries series = new ChartSeries("Depositos por inventores");

        List<Pair> i = repo.updateInventors(currentProject, limit, filtro);

        Collections.reverse(i);

        for (Pair pair : i) {
            String inventor = (String) pair.getKey();
            Integer count = (Integer) pair.getValue();
            series.set(inventor, count);
        }

        return series;
    }

    public InventorRepository getRepo() {
        return repo;
    }
}