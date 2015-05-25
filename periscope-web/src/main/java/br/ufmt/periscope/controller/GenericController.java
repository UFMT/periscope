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
 * Modelo abstrato generico dos controllers<BR/>
 * Quase todos os controllers de gráficos do sistema extendem essa classe
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
     * Método pós construtor da classe abstrata generica dos controllers<BR/>
     * Atualiza os parametros do filtro, o intervalo de datas existentes no projeto e o gráfico sendo mostrado atualmente
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
     * Método abstrato que atualiza os gráficos do sistema<BR/>
     * Apenas o cabeçalho definido nesta classe
     */
    public abstract void refreshChart();

    /**
     * 
     * @return
     */
    public PatentRepository getPatentRepository() {
        return patentRepository;
    }

    /**
     * 
     * @param patentRepository
     */
    public void setPatentRepository(PatentRepository patentRepository) {
        this.patentRepository = patentRepository;
    }

    /**
     * 
     * @return
     */
    public Date getMinDate() {
        return minDate;
    }

    /**
     * 
     * @param minDate
     */
    public void setMinDate(Date minDate) {
        this.minDate = minDate;
    }

    /**
     * 
     * @return
     */
    public Date getMaxDate() {
        return maxDate;
    }

    /**
     * 
     * @param maxDate
     */
    public void setMaxDate(Date maxDate) {
        this.maxDate = maxDate;
    }

    /**
     * 
     * @return
     */
    public Project getCurrentProject() {
        return currentProject;
    }

    /**
     * 
     * @param currentProject
     */
    public void setCurrentProject(Project currentProject) {
        this.currentProject = currentProject;
    }

    /**
     * 
     * @return
     */
    public CartesianChartModel getModel() {
        return model;
    }

    /**
     * 
     * @param model
     */
    public void setModel(CartesianChartModel model) {
        this.model = model;
    }

    /**
     * 
     * @return
     */
    public List<Pair> getPairs() {
        return pairs;
    }

    /**
     * 
     * @param pairs
     */
    public void setPairs(List<Pair> pairs) {
        this.pairs = pairs;
    }

    /**
     * 
     * @return
     */
    public Filters getFiltro() {
        return filtro;
    }

    /**
     * 
     * @param filtro
     */
    public void setFiltro(Filters filtro) {
        this.filtro = filtro;
    }

    /**
     * 
     * @return
     */
    public int getLimit() {
        return limit;
    }

    /**
     * 
     * @param limit
     */
    public void setLimit(int limit) {
        this.limit = limit;
    }

    /**
     * 
     * Filter's change listener
     * @param event
     */
    public void selectListener(ValueChangeEvent event) {
        int sel = getFiltro().getSelecionaData();
        int newSel=  (Integer)event.getNewValue();
        getFiltro().setInicio(getPatentRepository().getMinDate(getCurrentProject(), newSel));
        getFiltro().setFim(getPatentRepository().getMaxDate(getCurrentProject(), newSel));
        setMinDate(getFiltro().getInicio());
        setMaxDate(getFiltro().getFim());

    }
}
