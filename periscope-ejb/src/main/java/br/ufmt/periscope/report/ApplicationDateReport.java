package br.ufmt.periscope.report;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.model.chart.ChartSeries;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.repository.ApplicationDateRepository;

@Named
public class ApplicationDateReport {

	private @Inject	ApplicationDateRepository repo;

	public ChartSeries applicationDateSeries(Project currentProject) {
		ChartSeries series = new ChartSeries("Depositos por ano");

		List<Pair> i = repo.getApplicationsByDate(currentProject);
		
		for (Pair pair : i) {
			Integer year = Integer.parseInt((String)pair.getKey());
			//System.out.println(aux.get("applicationPerYear"));
			Integer count = (Integer)pair.getValue();
			series.set(year, count);
		}		

		return series;
	}

}
