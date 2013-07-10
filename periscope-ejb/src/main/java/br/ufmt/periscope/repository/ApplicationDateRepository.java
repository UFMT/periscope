package br.ufmt.periscope.repository;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.report.Pair;

import com.github.jmkgreen.morphia.Datastore;
import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

@Named
public class ApplicationDateRepository {

	private @Inject
	Datastore ds;

	public List<Pair> getApplicationsByDate(Project projetoAtual) {

		/**
		 * db.Patent.aggregate( {$match:{"project.$id":new
		 * ObjectId("51db042d44ae70d2d3649c20")}}, {$match:{blacklisted:false}},
		 * {$project:{ year1:{$year:"$applicationDate"}}},
		 * {$group:{_id:"$year1",ApplicationPerYear:{$sum:1}}}, {$sort:{_id:1}}
		 * );
		 */
		DBObject matchProj = new BasicDBObject();
		matchProj.put("$match", new BasicDBObject("project.$id", projetoAtual.getId()));

		DBObject matchBlacklist = new BasicDBObject();
		matchBlacklist.put("$match",new BasicDBObject("blacklisted", false));

		DBObject project = new BasicDBObject();
		project.put("$project", new BasicDBObject("year1", new BasicDBObject(
				"$year", "$applicationDate")));

		DBObject group = new BasicDBObject();
		DBObject fields = new BasicDBObject("_id", "$year1");
		fields.put("applicationPerYear", new BasicDBObject("$sum", 1));
		group.put("$group", fields);

		DBObject sort = new BasicDBObject("$sort",new BasicDBObject("_id", 1));
		
		AggregationOutput output = ds.getCollection(Patent.class).aggregate(
				matchProj, matchBlacklist, project, group, sort);
		
		
		BasicDBList outputResult = (BasicDBList) output.getCommandResult().get("result");
		
		List<Pair> pairs = new ArrayList<Pair>();
		for (Object object : outputResult) {
			DBObject aux = (DBObject) object;
			String year = aux.get("_id").toString();			
			Integer count = (Integer)aux.get("applicationPerYear");
			
			pairs.add(new Pair(year, count));
		}
		return pairs;
	}

}