package br.ufmt.periscope.controller;

import java.util.ArrayList;
import java.util.Collections;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;

import org.primefaces.model.chart.CartesianChartModel;
import org.primefaces.model.chart.ChartSeries;

import br.ufmt.periscope.report.MainIPCReport;
import br.ufmt.periscope.report.Pair;


@ManagedBean
@ViewScoped
public class MainIPCController extends GenericController {

    private @Inject
    MainIPCReport report;
    private boolean klass;
    private boolean subKlass;
    private boolean group;
    private boolean subGroup;
    private boolean description;

    @PostConstruct
    @Override
    public void init() {
        klass = false;
        subKlass = false;
        group = false;
        subGroup = false;

        setLimit(8);
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

        this.description = false;
        String description;
        for (Object key : series.getData().keySet()) {
            Number value = series.getData().get(key);
            String ipc = (String) key;
            description = null;
            if (ipc != null && ipc.length() == 1) {
                this.description = true;
                switch (ipc.charAt(0)) {
                    case 'A':
                        description = "Necessidades Humanas";
                        break;
                    case 'B':
                        description = "Operações de Processamento; Transporte";
                        break;
                    case 'C':
                        description = "Química e Metalurgia";
                        break;
                    case 'D':
                        description = "Têxteis e Papel";
                        break;
                    case 'F':
                        description = "Engenharia Mecânica; Iluminação; Aquecimento; Arma";
                        break;
                    case 'E':
                        description = "Construções Fixas";
                        break;
                    case 'G':
                        description = "Física";
                        break;
                    case 'H':
                        description = "Eletricidade";
                        break;
                }
            }
            getPairs().add(new Pair(key, value, description));
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

    public boolean isDescription() {
        return description;
    }

    public void setDescription(boolean description) {
        this.description = description;
    }
    
    
}
