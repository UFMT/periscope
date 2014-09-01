package br.ufmt.periscope.report;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.model.chart.ChartSeries;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.repository.PublicationDateRepository;
import br.ufmt.periscope.util.Filters;
import java.util.Collections;

@Named
public class PublicationDateReport {

	private @Inject	PublicationDateRepository repo;

	public ChartSeries publicationDateSeries(Project currentProject, Filters filtro) {
		ChartSeries series = new ChartSeries("Publicações por ano");
                
		List<Pair> i = repo.getPublicationsByDate(currentProject, filtro);
                
                Collections.reverse(i);
		
		for (Pair pair : i) {
			Integer year = Integer.parseInt((String)pair.getKey());
			//System.out.println(aux.get("applicationPerYear"));
			Integer count = (Integer)pair.getValue();
			series.set(year, count);
		}		

		return series;
	}

}
