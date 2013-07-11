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
public class ClassificationRepository {

	private @Inject
	Datastore ds;

	public List<Pair> getMainIPC(Project currentProject) {

		/**
		 * db.Patent.aggregate( {$match:{"project.$id":new
		 * ObjectId("51db042d44ae70d2d3649c20")}},
		 * {$match:{mainClassification:{$exists:true}}},
		 * {$match:{blacklisted:false}},
		 * {$project:{section:{$substr:["$mainClassification.klass",0,1]}}},
		 * {$group:{_id:"$section",applicationPerSector:{$sum:1}}},
		 * {$sort:{_id:1}} );
		 */

		DBObject matchProj = new BasicDBObject();
		matchProj.put("$match",
				new BasicDBObject("project.$id", currentProject.getId()));

		DBObject matchBlacklist = new BasicDBObject();
		matchBlacklist.put("$match", new BasicDBObject("blacklisted", false));

		DBObject matchMainClassificationExists = new BasicDBObject("$match",
				new BasicDBObject("mainClassification", new BasicDBObject(
						"$exists", true)));

		Object[] list = new Object[] { "$mainClassification.value", 0, 1 };
		DBObject section = new BasicDBObject("section", new BasicDBObject(
				"$substr", list));
		DBObject project = new BasicDBObject("$project", section);

		DBObject fields = new BasicDBObject("_id", "$section");
		fields.put("applicationPerSector", new BasicDBObject("$sum", 1));
		DBObject group = new BasicDBObject("$group", fields);

		DBObject sort = new BasicDBObject("$sort", new BasicDBObject("applicationPerSector", -1));

		AggregationOutput output = ds.getCollection(Patent.class).aggregate(
				matchProj, matchBlacklist, matchMainClassificationExists,
				project, group, sort);

		BasicDBList outputResult = (BasicDBList) output.getCommandResult().get(
				"result");

		List<Pair> pairs = new ArrayList<Pair>();
		for (Object object : outputResult) {
			DBObject aux = (DBObject) object;
			String ipc = aux.get("_id").toString();
			Integer count = (Integer) aux.get("applicationPerSector");

			pairs.add(new Pair(ipc, count));
		}
		return pairs;
	}

}
