package br.ufmt.periscope.controller;

import br.ufmt.periscope.report.Pair;
import br.ufmt.periscope.report.PriorityDateReport;
import java.util.ArrayList;
import java.util.Collections;
import javax.faces.bean.ManagedBean;
import javax.inject.Inject;
import org.primefaces.model.chart.CartesianChartModel;
import org.primefaces.model.chart.ChartSeries;

@ManagedBean
public class PriorityDateController extends GenericController {
    
    private @Inject PriorityDateReport report;
    

    @Override
    public void refreshChart() {
        setModel(new CartesianChartModel());
        ChartSeries series = report.priorityDateSeries(getCurrentProject(), getFiltro());
        getModel().addSeries(series);
        
        setPairs(new ArrayList<Pair>());
        
        for (Object key : series.getData().keySet()) {
            Number value = series.getData().get(key);
            getPairs().add(new Pair(key, value));
        }
        Collections.reverse(getPairs());
    }
    
}
