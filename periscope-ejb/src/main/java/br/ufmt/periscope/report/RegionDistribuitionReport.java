/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufmt.periscope.report;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.report.Pair;
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
        System.out.println("entrou report");
        List<Pair> i = repo.getRegionDistribuitions(currentProject);
        System.out.println("saiu report");
        Collections.reverse(i);

        for (Pair pair : i) {
            String region = (String) pair.getKey();
            //System.out.println(aux.get("applicationPerYear"));
            Integer count = (Integer) pair.getValue();
            series.set(region, count);
        }

        return series;
    }

}
