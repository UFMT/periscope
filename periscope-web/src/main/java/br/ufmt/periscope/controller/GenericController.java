package br.ufmt.periscope.controller;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.qualifier.CurrentProject;
import br.ufmt.periscope.report.Pair;
import br.ufmt.periscope.repository.PatentRepository;
import br.ufmt.periscope.util.Filters;
import java.util.Date;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.primefaces.model.chart.CartesianChartModel;

public abstract class GenericController {

    private @Inject
    @CurrentProject
    Project currentProject;
    private @Inject
    PatentRepository patentRepository;
    private CartesianChartModel model;
    private List<Pair> pairs;
    private @Inject
    Filters filtro;
    private int limit = 5;
    private Date minDate, maxDate;

    @PostConstruct
    public void init() {
        setMinDate(getPatentRepository().getMinDate(getCurrentProject()));
        setMaxDate(getPatentRepository().getMaxDate(getCurrentProject()));
        getFiltro().setComplete(false);
        getFiltro().setSelecionaData(0);
        getFiltro().setInicio(getPatentRepository().getMinDate(getCurrentProject()));
        getFiltro().setFim(getPatentRepository().getMaxDate(getCurrentProject()));

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
}
