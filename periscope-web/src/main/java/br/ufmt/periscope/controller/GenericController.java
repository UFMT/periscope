package br.ufmt.periscope.controller;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.qualifier.CurrentProject;
import br.ufmt.periscope.report.Pair;
import br.ufmt.periscope.repository.PatentRepository;
import br.ufmt.periscope.util.Filters;
import java.util.Date;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.event.ValueChangeEvent;
import javax.inject.Inject;
import org.primefaces.model.chart.CartesianChartModel;

/**
 * Abstract generic controller model<BR/>
 * All system's graph controllers extend this class
 */
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

    /**
     * Controller constructor<BR/>
     * Gets the date range according to the current open project's patents<BR/>
     * Also sets the filter's parameters and refreshes the chart shown
     */
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

    /**
     * Absctract method to refresh system's charts<BR/>
     * Only the header here
     */
    public abstract void refreshChart();

    /**
     * Patent Repository's getter
     * @return
     */
    public PatentRepository getPatentRepository() {
        return patentRepository;
    }

    /**
     * Patent Repository's setter
     * @param patentRepository
     */
    public void setPatentRepository(PatentRepository patentRepository) {
        this.patentRepository = patentRepository;
    }

    /**
     * Minimum date range's getter
     * @return
     */
    public Date getMinDate() {
        return minDate;
    }

    /**
     * Minimum date range's setter
     * @param minDate
     */
    public void setMinDate(Date minDate) {
        this.minDate = minDate;
    }

    /**
     * Maximum date range's getter
     * @return
     */
    public Date getMaxDate() {
        return maxDate;
    }

    /**
     * Maximum date range's setter
     * @param maxDate
     */
    public void setMaxDate(Date maxDate) {
        this.maxDate = maxDate;
    }

    /**
     * Open current project's getter
     * @return
     */
    public Project getCurrentProject() {
        return currentProject;
    }

    /**
     * Open current project's setter
     * @param currentProject
     */
    public void setCurrentProject(Project currentProject) {
        this.currentProject = currentProject;
    }

    /**
     * Chart model's getter
     * @return
     */
    public CartesianChartModel getModel() {
        return model;
    }

    /**
     * Chart model's setter
     * @param model
     */
    public void setModel(CartesianChartModel model) {
        this.model = model;
    }

    /**
     * Pair list's getter
     * @return
     */
    public List<Pair> getPairs() {
        return pairs;
    }

    /**
     * Pair list's setter
     * @param pairs
     */
    public void setPairs(List<Pair> pairs) {
        this.pairs = pairs;
    }

    /**
     * Filter's getter
     * @return
     */
    public Filters getFiltro() {
        return filtro;
    }

    /**
     * Filter's setter
     * @param filtro
     */
    public void setFiltro(Filters filtro) {
        this.filtro = filtro;
    }

    /**
     * Chart limit's getter
     * @return
     */
    public int getLimit() {
        return limit;
    }

    /**
     * Chart limit's setter
     * @param limit
     */
    public void setLimit(int limit) {
        this.limit = limit;
    }

    /**
     * Filter's change listener
     * @param event
     */
    public void selectListener(ValueChangeEvent event) {
        int sel = getFiltro().getSelecionaData();
        int newSel=  (Integer)event.getNewValue();
        getFiltro().setInicio(getPatentRepository().getMinDate(getCurrentProject(), newSel));
        getFiltro().setFim(getPatentRepository().getMaxDate(getCurrentProject(), newSel));

    }
}
