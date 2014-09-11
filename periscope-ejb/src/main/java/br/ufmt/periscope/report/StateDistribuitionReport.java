package br.ufmt.periscope.report;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.repository.StateDistribuitionRepository;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.model.chart.ChartSeries;

@Named
public class StateDistribuitionReport {

	private @Inject	StateDistribuitionRepository repo;
        

	public ChartSeries StateDistribuitionSeries(Project currentProject) {
		ChartSeries series = new ChartSeries("Distribuição por Estados");
//                System.out.println("entrou report");
		List<Pair> i = repo.getStateDistribuitions(currentProject);
//                System.out.println("saiu report");
                Collections.reverse(i);
		
		for (Pair pair : i) {
                        String state = (String)pair.getKey();
			//System.out.println(aux.get("applicationPerYear"));
			Integer count = (Integer)pair.getValue();
			series.set(state, count);
		}		

		return series;
	}

}