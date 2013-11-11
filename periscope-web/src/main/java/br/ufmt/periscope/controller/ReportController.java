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
import br.ufmt.periscope.util.Filters;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ManagedBean(name="report")
@ViewScoped
public class ReportController {		
 
	private @Inject @CurrentProject Project currentProject;
	private @Inject MainApplicantReport report;
	private CartesianChartModel model;
	private int limit = 5;
        private List<Pair> pairs;
        
        private @Inject Filters filtro;

	
	@PostConstruct
	public void init(){
                filtro.setComplete(false);
                filtro.setSelecionaData(0);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
        try {
            filtro.setInicio(simpleDateFormat.parse("01/01/2000"));
            filtro.setFim(simpleDateFormat.parse("31/12/2020"));
        } catch (ParseException ex) {
            Logger.getLogger(ReportController.class.getName()).log(Level.SEVERE, null, ex);
        }
                
		mainApplicantChart();
	}
	
	
	public void mainApplicantChart(){
						
		model = new CartesianChartModel();
		ChartSeries series = report.mainApplicantSeries(currentProject, limit, filtro);				
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

    public Filters getFiltro() {
        return filtro;
    }

    public void setFiltro(Filters filtro) {
        this.filtro = filtro;
    }
        
        
	
        
	
}
