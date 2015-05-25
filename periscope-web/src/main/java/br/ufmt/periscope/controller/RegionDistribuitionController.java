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

/**
 * - @ManagedBean<BR/>
 * - @ViewScoped<BR/>
 * Classe controller responsável por operações de visualização relacionadas à distribuição regional das patentes
 */
@ManagedBean
@ViewScoped
public class RegionDistribuitionController extends GenericController {

    private @Inject
    RegionDistribuitionReport report;

    /**
     * Método responsável por atualizar os gráficos relacionados à distribuição regional das patentes
     */
    @Override
    public void refreshChart() {

        setModel(new CartesianChartModel());
        ChartSeries series = report.RegionDistribuitionSeries(getCurrentProject());
        getModel().addSeries(series);

        setPairs(new ArrayList<Pair>());

        for (Object key : series.getData().keySet()) {
            Number value = series.getData().get(key);
            getPairs().add(new Pair(key, value));
        }

        Collections.reverse(getPairs());
    }

}
