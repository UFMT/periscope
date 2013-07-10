package br.ufmt.periscope.report;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.model.chart.ChartSeries;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.repository.InventorRepository;

@Named
public class MainInventorReport {
	
	private @Inject InventorRepository repo;
	
	public ChartSeries InventorDateSeries(Project currentProject, int limit) {
		ChartSeries series = new ChartSeries("Depositos por inventores");

		List<Pair> i = repo.getInventors(currentProject,limit);
		
		Collections.reverse(i);
		
		for (Pair pair : i) {
			String inventor = (String)pair.getKey();
			//System.out.println(aux.get("applicationPerYear"));
			Integer count = (Integer)pair.getValue();
			series.set(inventor, count);
		}		

		return series;
	}

}
