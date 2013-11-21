package br.ufmt.periscope.controller;

import java.util.ArrayList;
import java.util.Collections;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;

import org.primefaces.model.chart.CartesianChartModel;
import org.primefaces.model.chart.ChartSeries;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.qualifier.CurrentProject;
import br.ufmt.periscope.report.MainIPCReport;
import br.ufmt.periscope.report.Pair;

import br.ufmt.periscope.repository.PatentRepository;
import java.util.Date;

@ManagedBean
@ViewScoped
public class MainIPCController extends GenericController {

    private @Inject
    MainIPCReport report;
    private boolean klass;
    private boolean subKlass;
    private boolean group;
    private boolean subGroup;

    @PostConstruct
    @Override
    public void init() {
        klass = false;
        subKlass = false;
        group = false;
        subGroup = false;

        super.init();
    }

    public void update() {
        if (!klass) {
            // classe nao esta selecionada
            // buscar secao
            subKlass = false;
            group = false;
            subGroup = false;
        } else if (!subKlass) {
            // classe selecionada e subclasse nao esta
            // buscar classe
            group = false;
            subGroup = false;
        } else if (!group) {
            // classe e subclasse selecionadas e grupo nao selecionado
            // buscar subclasse
            subGroup = false;
        } else if (!subGroup) {
            // classe, subclasse e grupo selecionado, subgrupo nao selecioando
            // buscar grupo
        } else {
            // tudo selecionado
            // buscar subgrupo
        }

        refreshChart();
    }

    public void refreshChart() {
        setModel(new CartesianChartModel());
        ChartSeries series = report.ipcCount(getCurrentProject(), klass, subKlass,
                group, subGroup, getLimit(), getFiltro());

        getModel().addSeries(series);

        setPairs(new ArrayList<Pair>());

        for (Object key : series.getData().keySet()) {
            Number value = series.getData().get(key);
            getPairs().add(new Pair(key, value));
        }

        Collections.reverse(getPairs());
    }

    public boolean isKlass() {
        return klass;
    }

    public void setKlass(boolean klass) {
        this.klass = klass;
    }

    public boolean isSubKlass() {
        return subKlass;
    }

    public void setSubKlass(boolean subKlass) {
        this.subKlass = subKlass;
    }

    public boolean isGroup() {
        return group;
    }

    public void setGroup(boolean group) {
        this.group = group;
    }

    public boolean isSubGroup() {
        return subGroup;
    }

    public void setSubGroup(boolean subGroup) {
        this.subGroup = subGroup;
    }
}
