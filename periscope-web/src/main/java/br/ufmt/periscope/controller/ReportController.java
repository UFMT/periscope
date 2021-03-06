package br.ufmt.periscope.controller;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;

import org.primefaces.model.chart.CartesianChartModel;
import org.primefaces.model.chart.ChartSeries;

import br.ufmt.periscope.report.MainApplicantReport;
import br.ufmt.periscope.report.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 */
@ManagedBean(name = "report")
@ViewScoped
public class ReportController extends GenericController {

    private @Inject
    MainApplicantReport report;

    public void refreshChart() {

        setModel(new CartesianChartModel());
        ChartSeries series = report.mainApplicantSeries(getCurrentProject(), getLimit(), getFiltro());
        getModel().addSeries(series);

        setPairs(new ArrayList<Pair>());
        for (Object key : series.getData().keySet()) {
            Integer value = (Integer) series.getData().get(key);
            getPairs().add(new Pair(key, value));
        }

        Collections.reverse(getPairs());
    }

    /**
     *
     * @param query
     * @return
     */
    public List<String> getApplicants(String query) {
        List<String> teste = report.getRepo().getApplicants(getCurrentProject(), query);
        return teste;

    }
}
