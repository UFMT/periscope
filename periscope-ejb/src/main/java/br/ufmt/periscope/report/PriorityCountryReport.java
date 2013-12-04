package br.ufmt.periscope.report;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.repository.PriorityCountryRepository;
import br.ufmt.periscope.util.Filters;
import com.github.jmkgreen.morphia.Datastore;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.model.chart.ChartSeries;

@Named
public class PriorityCountryReport {

    private @Inject
    PriorityCountryRepository repo;
    private @Inject
    Datastore ds;

    public ChartSeries mainPriorityCountrySeries(Project currentProject, int limit, Filters filtro) {
        ChartSeries series = new ChartSeries("Países de Prioridade");

        List<Pair> i = repo.getPriorities(currentProject, limit, filtro);

        Collections.reverse(i);

        for (Pair pair : i) {
            String country = (String) pair.getKey();
            //System.out.println(aux.get("applicationPerYear"));
            Integer count = (Integer) pair.getValue();
            series.set(country, count);
        }

        return series;
    }
}
