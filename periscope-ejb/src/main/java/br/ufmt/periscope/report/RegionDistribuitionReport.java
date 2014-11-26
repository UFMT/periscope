package br.ufmt.periscope.report;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.repository.RegionDistribuitionRepository;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.model.chart.ChartSeries;

@Named
public class RegionDistribuitionReport {

    private @Inject
    RegionDistribuitionRepository repo;

    public ChartSeries RegionDistribuitionSeries(Project currentProject) {
        ChartSeries series = new ChartSeries("Distribuição por Regiões");
        List<Pair> i = repo.getRegionDistribuitions(currentProject);
        Collections.reverse(i);

        for (Pair pair : i) {
            String region = (String) pair.getKey();
            Integer count = (Integer) pair.getValue();
            series.set(region, count);
        }

        return series;
    }

}