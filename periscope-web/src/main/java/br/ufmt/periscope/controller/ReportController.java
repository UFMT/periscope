package br.ufmt.periscope.controller;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;

import org.primefaces.model.chart.CartesianChartModel;
import org.primefaces.model.chart.ChartSeries;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.qualifier.CurrentProject;
import br.ufmt.periscope.report.MainApplicantReport;

@ManagedBean(name="report")
@ViewScoped
public class ReportController {		
 
	private @Inject @CurrentProject Project currentProject;
	private @Inject MainApplicantReport report;
	private CartesianChartModel model;
	private int limit = 5;
	
	@PostConstruct
	public void init(){
		mainApplicantChart();
	}
	
	
	public void mainApplicantChart(){
						
		model = new CartesianChartModel();
		ChartSeries series = report.mainApplicantSeries(currentProject, limit);				
		model.addSeries(series);
	}


	public CartesianChartModel getModel() {
		return model;
	}


	public int getLimit() {
		return limit;
	}


	public void setLimit(int limit) {
		this.limit = limit;
	}
	
	
	
}
