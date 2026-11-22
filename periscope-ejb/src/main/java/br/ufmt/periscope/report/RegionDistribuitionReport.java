package br.ufmt.periscope.report;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.repository.RegionDistribuitionRepository;
import br.ufmt.periscope.util.Filters;
import java.util.Collections;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

@ApplicationScoped
@Named
public class RegionDistribuitionReport {

    private @Inject
    RegionDistribuitionRepository repo;

    public ChartSeries RegionDistribuitionSeries(Project currentProject) {
        return RegionDistribuitionSeries(currentProject, null);
    }

    public ChartSeries RegionDistribuitionSeries(Project currentProject, Filters filtro) {
        ChartSeries series = new ChartSeries("Distribuição por Regiões");
        List<Pair> i = repo.getRegionDistribuitions(currentProject, filtro);
        Collections.reverse(i);

        for (Pair pair : i) {
            String region = (String) pair.getKey();
            Integer count = (Integer) pair.getValue();
            series.set(region, count);
        }

        return series;
    }

}