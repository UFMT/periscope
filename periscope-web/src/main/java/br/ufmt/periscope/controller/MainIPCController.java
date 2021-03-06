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
import javax.faces.event.ValueChangeEvent;

/**
 * - @ManagedBean<BR/>
 * - @ViewScoped<BR/>
 * Classe controller responsável por operações de visualização relacionadas à classificação IPC
 */
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
    private int classification;

    /**
     * Método pós construção que foi sobrescrito para iniciar parametros do controller
     */
    @PostConstruct
    @Override
    public void init() {
        klass = false;
        subKlass = false;
        group = false;
        subGroup = false;
        this.setClassification(1);
        setLimit(8);
        super.init();
        
    }

    /**
     * Método que atualiza os valores dos filtros existentes para os gráficos da classificação IPC
     */
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

    /**
     * Método responsável pela atualização dos gráficos relacionados à classificação IPC
     */
    @Override
    public void refreshChart() {
        setModel(new CartesianChartModel());
        ChartSeries series = report.ipcCount(getCurrentProject(), klass, subKlass,
                group, subGroup, getLimit(), getFiltro(), this.getClassification());

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
                    case 'Y':
                        description = "";
                        break;
                }
            }
            getPairs().add(new Pair(key, value, description));
        }

        Collections.reverse(getPairs());
    }

    /**
     *
     * @return
     */
    public boolean isKlass() {
        return klass;
    }

    /**
     *
     * @param event
     */
    public void classificationListener(ValueChangeEvent event){
        int newVal = (Integer) event.getNewValue();
        this.setClassification(newVal);
    }

    /**
     *
     * @param klass
     */
    public void setKlass(boolean klass) {
        this.klass = klass;
    }

    /**
     *
     * @return
     */
    public boolean isSubKlass() {
        return subKlass;
    }

    /**
     *
     * @param subKlass
     */
    public void setSubKlass(boolean subKlass) {
        this.subKlass = subKlass;
    }

    /**
     *
     * @return
     */
    public boolean isGroup() {
        return group;
    }

    /**
     *
     * @param group
     */
    public void setGroup(boolean group) {
        this.group = group;
    }

    /**
     *
     * @return
     */
    public boolean isSubGroup() {
        return subGroup;
    }

    /**
     *
     * @param subGroup
     */
    public void setSubGroup(boolean subGroup) {
        this.subGroup = subGroup;
    }

    /**
     *
     * @return
     */
    public boolean isDescription() {
        return description;
    }

    /**
     *
     * @param description
     */
    public void setDescription(boolean description) {
        this.description = description;
    }

    /**
     *
     * @return
     */
    public int getClassification() {
        return classification;
    }

    /**
     *
     * @param classification
     */
    public void setClassification(int classification) {
        this.classification = classification;
    }
    
    
}
