package br.ufmt.periscope.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;

import org.primefaces.model.chart.CartesianChartModel;
import org.primefaces.model.chart.ChartSeries;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.qualifier.CurrentProject;
import br.ufmt.periscope.report.MainInventorReport;
import br.ufmt.periscope.report.Pair;
import br.ufmt.periscope.repository.PatentRepository;
import br.ufmt.periscope.util.Filters;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

@ManagedBean
@ViewScoped
public class MainInventorController {

	private @Inject	@CurrentProject Project currentProject;
	private @Inject	MainInventorReport report;
        private @Inject PatentRepository patentRepository;
	private CartesianChartModel model;
	private List<Pair> pairs;
        private @Inject Filters filtro;
        private Date minDate, maxDate;

	private int limit = 5;

    @PostConstruct
	public void init(){
                setMinDate(patentRepository.getMinDate(currentProject));
                setMaxDate(patentRepository.getMaxDate(currentProject));
                filtro.setComplete(false);
                filtro.setSelecionaData(0);
                filtro.setInicio(patentRepository.getMinDate(currentProject));
                filtro.setFim(patentRepository.getMaxDate(currentProject));
     
		mainInventorChart();
	}
	
	public void mainInventorChart(){
		
		model = new CartesianChartModel();
		ChartSeries series = report.InventorDateSeries(currentProject,limit, filtro);
		
		model.addSeries(series);
		
		setPairs(new ArrayList<Pair>());
		
		for(Object key : series.getData().keySet()){
			Number value = series.getData().get(key);
			getPairs().add(new Pair(key, value));
		}
		
		Collections.reverse(pairs);
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

    public Filters getFiltro() {
        return filtro;
    }

    public void setFiltro(Filters filtro) {
        this.filtro = filtro;
    }
        
        

}
