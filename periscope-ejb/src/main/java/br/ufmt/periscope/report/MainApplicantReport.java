package br.ufmt.periscope.report;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.model.chart.ChartSeries;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.repository.ApplicantRepository;
import br.ufmt.periscope.util.Filters;

import com.github.jmkgreen.morphia.Datastore;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

@Named
public class MainApplicantReport {

	private @Inject ApplicantRepository repo;
	private @Inject Datastore ds;
	
	public ChartSeries mainApplicantSeries(Project currentProject, int limit, Filters filtro){
		ChartSeries series = new ChartSeries("Número de Depositos");
                repo.updateMainApplicants(currentProject, filtro);		
		List<DBObject> it = ds.getDB()
							  .getCollection("mainApplicant").find()
							  .sort(new BasicDBObject("value",-1))
							  .limit(limit)
							  .toArray();
		
		Collections.reverse(it);
		
		for(DBObject obj : it){
			String name = (String) obj.get("_id");
			Double count = (Double) obj.get("value");
//                        System.out.println("COut: "+count);
			series.set(name,count.intValue());					
		}				
		
		return series;
	}

    public ApplicantRepository getRepo() {
        return repo;
    }
        

}

