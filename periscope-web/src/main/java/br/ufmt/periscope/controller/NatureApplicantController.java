package br.ufmt.periscope.controller;

import java.util.ArrayList;
import java.util.Collections;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import org.primefaces.model.chart.CartesianChartModel;
import org.primefaces.model.chart.ChartSeries;
import br.ufmt.periscope.report.MainInventorReport;
import br.ufmt.periscope.report.MainNatureApplicantReport;
import br.ufmt.periscope.report.Pair;

@ManagedBean
@ViewScoped
public class NatureApplicantController extends GenericController{


	private @Inject	MainNatureApplicantReport report;
	
        @Override
	public void refreshChart(){
		
		setModel(new CartesianChartModel());
		ChartSeries series = report.NatureApplicantSeries(getCurrentProject(), getFiltro());
		
		getModel().addSeries(series);
		
		setPairs(new ArrayList<Pair>());
		
		for(Object key : series.getData().keySet()){
			Number value = series.getData().get(key);
			getPairs().add(new Pair(key, value));
		}
		
		Collections.reverse(getPairs());
	}

}
