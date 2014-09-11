/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufmt.periscope.controller;

import br.ufmt.periscope.report.Pair;
import br.ufmt.periscope.report.RegionDistribuitionReport;
import java.util.ArrayList;
import java.util.Collections;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import org.primefaces.model.chart.CartesianChartModel;
import org.primefaces.model.chart.ChartSeries;

@ManagedBean
@ViewScoped
public class RegionDistribuitionController extends GenericController {

    private @Inject
    RegionDistribuitionReport report;

    public void refreshChart() {
        
        setModel(new CartesianChartModel());
//        System.out.println("entrou controller");
        ChartSeries series = report.RegionDistribuitionSeries(getCurrentProject());
//        System.out.println("saiu controller");
        getModel().addSeries(series);

        setPairs(new ArrayList<Pair>());

        for (Object key : series.getData().keySet()) {
            Number value = series.getData().get(key);
            getPairs().add(new Pair(key, value));
        }

        Collections.reverse(getPairs());
    }

}
