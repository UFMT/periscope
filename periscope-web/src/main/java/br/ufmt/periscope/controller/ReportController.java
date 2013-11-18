package br.ufmt.periscope.controller;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;

import org.primefaces.model.chart.CartesianChartModel;
import org.primefaces.model.chart.ChartSeries;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.qualifier.CurrentProject;
import br.ufmt.periscope.report.MainApplicantReport;
import br.ufmt.periscope.report.Pair;

import br.ufmt.periscope.repository.PatentRepository;
import java.util.Date;
import java.util.ArrayList;
import java.util.Collections;;



@ManagedBean(name = "report")
@ViewScoped
public class ReportController extends GenericController {

    private @Inject
    MainApplicantReport report;

    @PostConstruct
    public void init() {
        setMinDate(getPatentRepository().getMinDate(getCurrentProject()));
        setMaxDate(getPatentRepository().getMaxDate(getCurrentProject()));
        getFiltro().setComplete(false);
        getFiltro().setSelecionaData(0);
        getFiltro().setInicio(getPatentRepository().getMinDate(getCurrentProject()));
        getFiltro().setFim(getPatentRepository().getMaxDate(getCurrentProject()));

        mainApplicantChart();
    }

    public void mainApplicantChart() {

        setModel(new CartesianChartModel());
        ChartSeries series = report.mainApplicantSeries(getCurrentProject(), getLimit(), getFiltro());
        getModel().addSeries(series);

        setPairs(new ArrayList<Pair>());
        for (Object key : series.getData().keySet()) {
            Number value = series.getData().get(key);
            getPairs().add(new Pair(key, value));
        }

        Collections.reverse(getPairs());
    }
}
