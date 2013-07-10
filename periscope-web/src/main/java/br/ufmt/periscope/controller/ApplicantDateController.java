package br.ufmt.periscope.controller;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;

import org.primefaces.model.chart.CartesianChartModel;
import org.primefaces.model.chart.ChartSeries;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.qualifier.CurrentProject;
import br.ufmt.periscope.report.ApplicationDateReport;
import br.ufmt.periscope.report.Pair;

@ManagedBean(name="applicantDateReport")
@ViewScoped
public class ApplicantDateController {
	
	private @Inject @CurrentProject Project currentProject;
	private @Inject ApplicationDateReport report;
	private CartesianChartModel model;
	private List<Pair> pairs;
	
	@PostConstruct
	public void init(){		
		model = new CartesianChartModel();
		ChartSeries series = report.applicationDateSeries(currentProject);		
		model.addSeries(series);
		
		setPairs(new ArrayList<Pair>());
		
		for(Object key : series.getData().keySet()){
			Number value = series.getData().get(key);
			getPairs().add(new Pair(key, value));
		}
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

}
