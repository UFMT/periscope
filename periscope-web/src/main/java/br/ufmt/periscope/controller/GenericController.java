package br.ufmt.periscope.controller;

import br.ufmt.periscope.model.Country;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.qualifier.CurrentProject;
import br.ufmt.periscope.report.Pair;
import br.ufmt.periscope.repository.PatentRepository;
import br.ufmt.periscope.util.Filters;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.event.ValueChangeEvent;
import javax.inject.Inject;
import org.primefaces.model.chart.CartesianChartModel;

public abstract class GenericController {

    private @Inject
    @CurrentProject
    Project currentProject;
    private @Inject
    PatentRepository patentRepository;
    private CartesianChartModel model;
    private Date minDate, maxDate;
    private List<Pair> pairs;
    private @Inject
    Filters filtro;
    private int limit = 5;

    @PostConstruct
    public void init() {
        setMinDate(getPatentRepository().getMinDate(getCurrentProject(), 1));
        setMaxDate(getPatentRepository().getMaxDate(getCurrentProject(), 1));
        getFiltro().setComplete(false);
        getFiltro().setSelecionaData(1);
        getFiltro().setInicio(getMinDate());
        getFiltro().setFim(getMaxDate());

        refreshChart();
    }

    public abstract void refreshChart();

    public PatentRepository getPatentRepository() {
        return patentRepository;
    }

    public void setPatentRepository(PatentRepository patentRepository) {
        this.patentRepository = patentRepository;
    }

    public Date getMinDate() {
        return minDate;
    }

    public void setMinDate(Date minDate) {
        this.minDate = minDate;
    }

    public Date getMaxDate() {
        return maxDate;
    }

    public void setMaxDate(Date maxDate) {
        this.maxDate = maxDate;
    }

    public Project getCurrentProject() {
        return currentProject;
    }

    public void setCurrentProject(Project currentProject) {
        this.currentProject = currentProject;
    }

    public CartesianChartModel getModel() {
        return model;
    }

    public void setModel(CartesianChartModel model) {
        this.model = model;
    }

    public List<Pair> getPairs() {
        return pairs;
    }

    public void setPairs(List<Pair> pairs) {
        this.pairs = pairs;
    }

    public Filters getFiltro() {
        return filtro;
    }

    public void setFiltro(Filters filtro) {
        this.filtro = filtro;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public void selectListener(ValueChangeEvent event) {
        int sel = getFiltro().getSelecionaData();
        int newSel=  (Integer)event.getNewValue();
//        System.out.println("trocando data:" + event.getNewValue() + " sel:" + sel);
        getFiltro().setInicio(getPatentRepository().getMinDate(getCurrentProject(), newSel));
        getFiltro().setFim(getPatentRepository().getMaxDate(getCurrentProject(), newSel));

    }
}
