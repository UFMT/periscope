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
 * - @ManagedBean<BR/>
 * - @ViewScoped<BR/>
 * Classe controller responsável por operações de visualização relacionadas aos depositantes
 */
@ManagedBean
@ViewScoped
public class MainApplicantController extends GenericController {

    private @Inject
    MainApplicantReport report;

    /**
     * Método responsável por atualizar os gráficos relacionados aos principais depositantes
     */
    @Override
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
     * Lista de depositantes dado um filtro
     * @param query String que deverá começar o nome dos depositantes
     * @return lista de depositantes do projeto nas quais os nomes começam com o parametro passado
     */
    public List<String> getApplicants(String query) {
        List<String> teste = report.getRepo().getApplicants(getCurrentProject(), query);
        return teste;

    }
}
