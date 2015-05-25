package br.ufmt.periscope.controller;

import java.util.ArrayList;
import java.util.Collections;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import org.primefaces.model.chart.CartesianChartModel;
import org.primefaces.model.chart.ChartSeries;
import br.ufmt.periscope.report.MainInventorReport;
import br.ufmt.periscope.report.Pair;
import java.util.List;

/**
 * - @ManagedBean<BR/>
 * - @ViewScoped<BR/>
 * Classe controller responsável por operações de visualização relacionadas aos inventores
 */
@ManagedBean
@ViewScoped
public class MainInventorController extends GenericController {

    private @Inject
    MainInventorReport report;

    /**
     * Método responsável pela atualização dos gráficos relacionados aos principais inventores
     */
    @Override
    public void refreshChart() {

        setModel(new CartesianChartModel());
        ChartSeries series = report.InventorDateSeries(getCurrentProject(), getLimit(), getFiltro());

        getModel().addSeries(series);

        setPairs(new ArrayList<Pair>());

        for (Object key : series.getData().keySet()) {
            Number value = series.getData().get(key);
            getPairs().add(new Pair(key, value));
        }

        Collections.reverse(getPairs());
    }

    /**
     * Lista de inventores dado um filtro
     * @param query String que deverá começar o nome de um inventor
     * @return lista de inventores do projeto nas quais os nomes começam com o paramtro passado
     */
    public List<String> getInventors(String query) {
        List<String> teste = report.getRepo().getInventors(getCurrentProject(), query);
        return teste;
    }
}
