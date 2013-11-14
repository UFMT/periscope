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
import br.ufmt.periscope.repository.PatentRepository;
import br.ufmt.periscope.util.Filters;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@ManagedBean(name="report")
@ViewScoped
public class ReportController {		
 
	private @Inject @CurrentProject Project currentProject;
	private @Inject MainApplicantReport report;
	private CartesianChartModel model;
	private int limit = 5;
        private List<Pair> pairs;
        private @Inject PatentRepository patentRepository;
        private @Inject Filters filtro;
        private Date minDate, maxDate;
	
	@PostConstruct
	public void init(){
            
                setMinDate(patentRepository.getMinDate(currentProject));
                setMaxDate(patentRepository.getMaxDate(currentProject));
                filtro.setComplete(false);
                filtro.setSelecionaData(0);
                filtro.setInicio(patentRepository.getMinDate(currentProject));
                filtro.setFim(patentRepository.getMaxDate(currentProject));
        
                
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

        public PatentRepository getPatentRepository() {
                return patentRepository;
        }

        public void setPatentRepository(PatentRepository pr) {
                this.patentRepository = pr;
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
