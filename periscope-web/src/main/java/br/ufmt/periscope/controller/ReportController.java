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
import br.ufmt.periscope.report.Pair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@ManagedBean(name="report")
@ViewScoped
public class ReportController {		
 
	private @Inject @CurrentProject Project currentProject;
	private @Inject MainApplicantReport report;
	private CartesianChartModel model;
	private int limit = 5;
        private List<Pair> pairs;

	
	@PostConstruct
	public void init(){
		mainApplicantChart();
	}
	
	
	public void mainApplicantChart(){
						
		model = new CartesianChartModel();
		ChartSeries series = report.mainApplicantSeries(currentProject, limit);				
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


	public int getLimit() {
		return limit;
	}


	public void setLimit(int limit) {
		this.limit = limit;
	}
	
        public List<Pair> getPairs() {
            return pairs;
        }

        public void setPairs(List<Pair> pairs) {
            this.pairs = pairs;
        }
	
	
}
