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

@ManagedBean
@ViewScoped
public class MainInventorController {

	private @Inject	@CurrentProject
	Project currentProject;
	private @Inject	MainInventorReport report;
	private CartesianChartModel model;
	private List<Pair> pairs;
	
	private int limit = 5;
	
	@PostConstruct
	public void init(){
		mainInventorChart();
	}
	
	public void mainInventorChart(){
		
		model = new CartesianChartModel();
		ChartSeries series = report.InventorDateSeries(currentProject,limit);
		
		model.addSeries(series);
		
		setPairs(new ArrayList<Pair>());
		
		for(Object key : series.getData().keySet()){
			Number value = series.getData().get(key);
			getPairs().add(new Pair(key, value));
		}
		
		Collections.reverse(pairs);
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

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

}
