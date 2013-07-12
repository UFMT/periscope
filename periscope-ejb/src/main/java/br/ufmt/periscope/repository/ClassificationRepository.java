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

	public List<Pair> getMainIPC(Project currentProject, boolean klass,
			boolean subKlass, boolean group, boolean subGroup, int limit) {

		AggregationOutput output;

		if (!klass) {
			// classe nao esta selecionada
			// buscar secao
			output = getSection(currentProject);
			subKlass = false;
			group = false;
			subGroup = false;
		} else if (!subKlass) {
			// classe selecionada e subclasse nao esta
			// buscar classe
			output = getKlass(currentProject, limit);
			group = false;
			subGroup = false;
		} else if (!group) {
			// classe e subclasse selecionadas e grupo nao selecionado
			// buscar subclasse
			output = getSubKlass(currentProject, limit);
			subGroup = false;
		} else if (!subGroup) {
			// classe, subclasse e grupo selecionado, subgrupo nao selecioando
			// buscar grupo
			output = getGroup(currentProject, limit);
		} else {
			// tudo selecionado
			// buscar subgrupo
			output = getSubGroup(currentProject, limit);
		}

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

	private AggregationOutput getSection(Project currentProject) {
		/**
		 * db.Patent.aggregate( {$match:{"project.$id":new
		 * ObjectId("51db042d44ae70d2d3649c20")}},
		 * {$match:{mainClassification:{$exists:true}}},
		 * {$match:{blacklisted:false}},
		 * {$project:{section:{$substr:["$mainClassification.klass",0,1]}}},
		 * {$group:{_id:"$section",applicationPerSector:{$sum:1}}},
		 * {$sort:{_id:1}} );
		 */

		// repete para todos
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

		DBObject sort = new BasicDBObject("$sort", new BasicDBObject(
				"applicationPerSector", -1));

		return ds.getCollection(Patent.class).aggregate(matchProj,
				matchBlacklist, matchMainClassificationExists, project, group,
				sort);
	}

	private AggregationOutput getKlass(Project currentProject, int limit) {
		/**
		 * db.Patent.aggregate( {$match:{"project.$id":new
		 * ObjectId("51db042d44ae70d2d3649c20")}},
		 * {$match:{mainClassification:{$exists:true}}},
		 * {$match:{blacklisted:false}},
		 * {$project:{section:{$substr:["$mainClassification.klass",0,3]}}},
		 * {$group:{_id:"$section",applicationPerSector:{$sum:1}}},
		 * {$sort:{applicationPerSector:-1}} );
		 */

		DBObject matchProj = new BasicDBObject();
		matchProj.put("$match",
				new BasicDBObject("project.$id", currentProject.getId()));

		DBObject matchBlacklist = new BasicDBObject();
		matchBlacklist.put("$match", new BasicDBObject("blacklisted", false));

		DBObject matchMainClassificationExists = new BasicDBObject("$match",
				new BasicDBObject("mainClassification", new BasicDBObject(
						"$exists", true)));

		Object[] list = new Object[] { "$mainClassification.klass", 0, 3 };
		DBObject section = new BasicDBObject("section", new BasicDBObject(
				"$substr", list));
		DBObject project = new BasicDBObject("$project", section);

		DBObject fields = new BasicDBObject("_id", "$section");
		fields.put("applicationPerSector", new BasicDBObject("$sum", 1));
		DBObject group = new BasicDBObject("$group", fields);

		DBObject sort = new BasicDBObject("$sort", new BasicDBObject(
				"applicationPerSector", -1));
		
		DBObject limit2 = new BasicDBObject("$limit",limit);

		return ds.getCollection(Patent.class).aggregate(matchProj,
				matchBlacklist, matchMainClassificationExists, project, group,
				sort, limit2);
	}

	private AggregationOutput getSubKlass(Project currentProject, int limit) {
		/**
		 * db.Patent.aggregate( {$match:{"project.$id":new
		 * ObjectId("51db042d44ae70d2d3649c20")}},
		 * {$match:{mainClassification:{$exists:true}}},
		 * {$match:{blacklisted:false}},
		 * {$group:{_id:"$mainClassification.klass"
		 * ,applicationPerSector:{$sum:1}}}, {$sort:{applicationPerSector:-1}}
		 * );
		 */

		DBObject matchProj = new BasicDBObject();
		matchProj.put("$match",
				new BasicDBObject("project.$id", currentProject.getId()));

		DBObject matchBlacklist = new BasicDBObject();
		matchBlacklist.put("$match", new BasicDBObject("blacklisted", false));

		DBObject matchMainClassificationExists = new BasicDBObject("$match",
				new BasicDBObject("mainClassification", new BasicDBObject(
						"$exists", true)));

		DBObject fields = new BasicDBObject("_id", "$mainClassification.klass");
		fields.put("applicationPerSector", new BasicDBObject("$sum", 1));
		DBObject group = new BasicDBObject("$group", fields);

		DBObject sort = new BasicDBObject("$sort", new BasicDBObject(
				"applicationPerSector", -1));
		
		DBObject limit2 = new BasicDBObject("$limit",limit);

		return ds.getCollection(Patent.class).aggregate(matchProj,
				matchMainClassificationExists, matchBlacklist, group, sort, limit2);

	}

	private AggregationOutput getGroup(Project currentProject, int limit) {
		/**
		 * db.Patent.aggregate( {$match:{"project.$id":new
		 * ObjectId("51db042d44ae70d2d3649c20")}},
		 * {$match:{mainClassification:{$exists:true}}},
		 * {$match:{blacklisted:false}},
		 * {$project:{group:{$concat:["$mainClassification.klass"
		 * ,"$mainClassification.group"]}}},
		 * {$group:{_id:"$group",applicationPerSector:{$sum:1}}},
		 * {$sort:{applicationPerSector:-1}} );
		 */
		DBObject matchProj = new BasicDBObject();
		matchProj.put("$match",
				new BasicDBObject("project.$id", currentProject.getId()));

		DBObject matchBlacklist = new BasicDBObject();
		matchBlacklist.put("$match", new BasicDBObject("blacklisted", false));

		DBObject matchMainClassificationExists = new BasicDBObject("$match",
				new BasicDBObject("mainClassification", new BasicDBObject(
						"$exists", true)));

		Object[] list = { "$mainClassification.klass",
				"$mainClassification.group" };
		DBObject section = new BasicDBObject("group", new BasicDBObject(
				"$concat", list));
		DBObject project = new BasicDBObject("$project", section);

		DBObject fields = new BasicDBObject("_id", "$group");
		fields.put("applicationPerSector", new BasicDBObject("$sum", 1));
		DBObject group = new BasicDBObject("$group", fields);

		DBObject sort = new BasicDBObject("$sort", new BasicDBObject(
				"applicationPerSector", -1));
		
		DBObject limit2 = new BasicDBObject("$limit",limit);

		return ds.getCollection(Patent.class).aggregate(matchProj,
				matchMainClassificationExists, matchBlacklist, project, group,
				sort, limit2);

	}

	private AggregationOutput getSubGroup(Project currentProject, int limit) {

		/**
		 * db.Patent.aggregate( {$match:{"project.$id":new
		 * ObjectId("51db042d44ae70d2d3649c20")}},
		 * {$match:{mainClassification:{$exists:true}}},
		 * {$match:{blacklisted:false}},
		 * {$project:{subgroup:{$concat:["$mainClassification.klass"
		 * ,"$mainClassification.group","/","$mainClassification.subgroup"]}}},
		 * {$group:{_id:"$subgroup",applicationPerSector:{$sum:1}}},
		 * {$sort:{applicationPerSector:-1}} );
		 */

		DBObject matchProj = new BasicDBObject();
		matchProj.put("$match",
				new BasicDBObject("project.$id", currentProject.getId()));

		DBObject matchBlacklist = new BasicDBObject();
		matchBlacklist.put("$match", new BasicDBObject("blacklisted", false));

		DBObject matchMainClassificationExists = new BasicDBObject("$match",
				new BasicDBObject("mainClassification", new BasicDBObject(
						"$exists", true)));

		Object[] list = { "$mainClassification.klass",
				"$mainClassification.group", "/",
				"$mainClassification.subgroup" };
		DBObject section = new BasicDBObject("group", new BasicDBObject(
				"$concat", list));
		DBObject project = new BasicDBObject("$project", section);

		DBObject fields = new BasicDBObject("_id", "$group");
		fields.put("applicationPerSector", new BasicDBObject("$sum", 1));
		DBObject group = new BasicDBObject("$group", fields);

		DBObject sort = new BasicDBObject("$sort", new BasicDBObject(
				"applicationPerSector", -1));
		
		DBObject limit2 = new BasicDBObject("$limit",limit);

		return ds.getCollection(Patent.class).aggregate(matchProj,
				matchMainClassificationExists, matchBlacklist, project, group,
				sort, limit2);

	}
}
