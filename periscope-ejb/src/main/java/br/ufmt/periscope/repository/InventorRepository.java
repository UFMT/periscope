package br.ufmt.periscope.repository;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.report.Pair;
import br.ufmt.periscope.util.Filters;

import com.github.jmkgreen.morphia.Datastore;
import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

@Named
public class InventorRepository {

	private @Inject
	Datastore ds;

	public List<Pair> getInventors(Project currentProject,int limit, Filters filtro) {

		/**
		 * db.Patent.aggregate( {$match:{"project.$id":new
		 * ObjectId("51db042d44ae70d2d3649c20")}}, {$match:{blacklisted:false}},
		 * {$unwind:"$inventors"},
		 * {$group:{_id:"$inventors",applicationPerInventor:{$sum:1}}},
		 * {$sort:{applicationPerInventor:-1}}, { $limit : 5 } );
		 */

		DBObject matchProj = new BasicDBObject();
		matchProj.put("$match",
				new BasicDBObject("project.$id", currentProject.getId()));
                
                DBObject matchComplete = new BasicDBObject();
                matchComplete.put("$match", new BasicDBObject("completed", filtro.isComplete()));
                
                DBObject matchDate = new BasicDBObject();
                if (filtro.getSelecionaData() == 1){
                    matchDate.put("$match", new BasicDBObject("publicationDate", new BasicDBObject("$gt", filtro.getInicio()).append("$lt", filtro.getFim())));
                }
                else{
                    matchDate.put("$match", new BasicDBObject("applicationDate", new BasicDBObject("$gt", filtro.getInicio()).append("$lt", filtro.getFim())));
                }

		DBObject matchBlacklist = new BasicDBObject();
		matchBlacklist.put("$match", new BasicDBObject("blacklisted", false));

		DBObject unwind = new BasicDBObject("$unwind", "$inventors");

		DBObject group = new BasicDBObject();
		DBObject fields = new BasicDBObject("_id", "$inventors");
		fields.put("applicationPerInventor", new BasicDBObject("$sum", 1));
		group.put("$group", fields);

		DBObject sort = new BasicDBObject("$sort", new BasicDBObject(
				"applicationPerInventor", -1));

                System.out.println(limit);
		DBObject pipeLimit = new BasicDBObject("$limit", limit);

		AggregationOutput output = ds.getCollection(Patent.class).aggregate(
				matchProj, matchComplete, matchDate, matchBlacklist, unwind, group, sort, pipeLimit);

		BasicDBList outputResult = (BasicDBList) output.getCommandResult().get(
				"result");

		List<Pair> pairs = new ArrayList<Pair>();
		for (Object object : outputResult) {
			DBObject aux = (DBObject) object;
			DBObject inventorName = (DBObject) aux.get("_id");
			String inventor = inventorName.get("name").toString();
			Integer count = (Integer) aux.get("applicationPerInventor");

			pairs.add(new Pair(inventor, count));
		}
		return pairs;
	}
}
