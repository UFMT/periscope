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
public class InventorRepository {

	private @Inject
	Datastore ds;

	public List<Pair> getInventors(Project currentProject,int limit) {

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

		DBObject matchBlacklist = new BasicDBObject();
		matchBlacklist.put("$match", new BasicDBObject("blacklisted", false));

		DBObject unwind = new BasicDBObject("$unwind", "$inventors");

		DBObject group = new BasicDBObject();
		DBObject fields = new BasicDBObject("_id", "$inventors");
		fields.put("applicationPerInventor", new BasicDBObject("$sum", 1));
		group.put("$group", fields);

		DBObject sort = new BasicDBObject("$sort", new BasicDBObject(
				"applicationPerInventor", -1));

		DBObject pipeLimit = new BasicDBObject("$limit", limit);

		AggregationOutput output = ds.getCollection(Patent.class).aggregate(
				matchProj, matchBlacklist, unwind, group, sort, pipeLimit);

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
