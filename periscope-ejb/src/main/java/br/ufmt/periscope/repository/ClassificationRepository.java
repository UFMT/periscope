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
public class ClassificationRepository {

    private @Inject
    Datastore ds;

    public List<Pair> getMainIPC(Project currentProject, boolean klass,
            boolean subKlass, boolean group, boolean subGroup, int limit, Filters filtro) {

        AggregationOutput output;

        if (!klass) {
            // classe nao esta selecionada
            // buscar secao
            output = getSection(currentProject, limit, filtro);
            subKlass = false;
            group = false;
            subGroup = false;
        } else if (!subKlass) {
            // classe selecionada e subclasse nao esta
            // buscar classe
            output = getKlass(currentProject, limit, filtro);
            group = false;
            subGroup = false;
        } else if (!group) {
            // classe e subclasse selecionadas e grupo nao selecionado
            // buscar subclasse
            output = getSubKlass(currentProject, limit, filtro);
            subGroup = false;
        } else if (!subGroup) {
            // classe, subclasse e grupo selecionado, subgrupo nao selecioando
            // buscar grupo
            output = getGroup(currentProject, limit, filtro);
        } else {
            // tudo selecionado
            // buscar subgrupo
            output = getSubGroup(currentProject, limit, filtro);
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

    private AggregationOutput getSection(Project currentProject, int limit, Filters filtro) {
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
        ArrayList<DBObject> parametros = new ArrayList<DBObject>();

        DBObject matchProj = new BasicDBObject();
        matchProj.put("$match",
                new BasicDBObject("project.$id", currentProject.getId()));

        if (filtro.isComplete()) {
            DBObject matchComplete = new BasicDBObject();
            matchComplete.put("$match", new BasicDBObject("completed", filtro.isComplete()));
            parametros.add(matchComplete);
        }

        DBObject matchDate = new BasicDBObject();
        if (filtro.getSelecionaData() == 0) {
            matchDate.put("$match", new BasicDBObject("publicationDate", new BasicDBObject("$gte", filtro.getInicio()).append("$lte", filtro.getFim())));
        } else {
            matchDate.put("$match", new BasicDBObject("applicationDate", new BasicDBObject("$gte", filtro.getInicio()).append("$lte", filtro.getFim())));
        }
        parametros.add(matchDate);

        DBObject matchBlacklist = new BasicDBObject();
        matchBlacklist.put("$match", new BasicDBObject("blacklisted", false));
        parametros.add(matchBlacklist);
        DBObject matchMainClassificationExists = new BasicDBObject("$match",
                new BasicDBObject("mainClassification", new BasicDBObject(
                "$exists", true)));
        parametros.add(matchMainClassificationExists);

        Object[] list = new Object[]{"$mainClassification.value", 0, 1};
        DBObject section = new BasicDBObject("section", new BasicDBObject(
                "$substr", list));
        DBObject project = new BasicDBObject("$project", section);
        parametros.add(project);
        DBObject fields = new BasicDBObject("_id", "$section");
        fields.put("applicationPerSector", new BasicDBObject("$sum", 1));
        DBObject group = new BasicDBObject("$group", fields);
        parametros.add(group);
        DBObject sort = new BasicDBObject("$sort", new BasicDBObject(
                "applicationPerSector", -1));
        parametros.add(sort);
        DBObject limit2 = new BasicDBObject("$limit", limit);
        parametros.add(limit2);

        DBObject[] parameters = new  DBObject[parametros.size()];
        parameters = parametros.toArray(parameters);
        return ds.getCollection(Patent.class).aggregate(matchProj,parameters);
    }

    private AggregationOutput getKlass(Project currentProject, int limit, Filters filtro) {
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

        DBObject matchComplete = new BasicDBObject();
        matchComplete.put("$match", new BasicDBObject("completed", filtro.isComplete()));

        DBObject matchDate = new BasicDBObject();
        if (filtro.getSelecionaData() == 1) {
            matchDate.put("$match", new BasicDBObject("publicationDate", new BasicDBObject("$gt", filtro.getInicio()).append("$lt", filtro.getFim())));
        } else {
            matchDate.put("$match", new BasicDBObject("applicationDate", new BasicDBObject("$gt", filtro.getInicio()).append("$lt", filtro.getFim())));
        }

        DBObject matchBlacklist = new BasicDBObject();
        matchBlacklist.put("$match", new BasicDBObject("blacklisted", false));

        DBObject matchMainClassificationExists = new BasicDBObject("$match",
                new BasicDBObject("mainClassification", new BasicDBObject(
                "$exists", true)));

        Object[] list = new Object[]{"$mainClassification.klass", 0, 3};
        DBObject section = new BasicDBObject("section", new BasicDBObject(
                "$substr", list));
        DBObject project = new BasicDBObject("$project", section);

        DBObject fields = new BasicDBObject("_id", "$section");
        fields.put("applicationPerSector", new BasicDBObject("$sum", 1));
        DBObject group = new BasicDBObject("$group", fields);

        DBObject sort = new BasicDBObject("$sort", new BasicDBObject(
                "applicationPerSector", -1));

        DBObject limit2 = new BasicDBObject("$limit", limit);

        return ds.getCollection(Patent.class).aggregate(matchProj, matchComplete, matchDate,
                matchBlacklist, matchMainClassificationExists, project, group,
                sort, limit2);
    }

    private AggregationOutput getSubKlass(Project currentProject, int limit, Filters filtro) {
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

        DBObject matchComplete = new BasicDBObject();
        matchComplete.put("$match", new BasicDBObject("completed", filtro.isComplete()));

        DBObject matchDate = new BasicDBObject();
        if (filtro.getSelecionaData() == 1) {
            matchDate.put("$match", new BasicDBObject("publicationDate", new BasicDBObject("$gt", filtro.getInicio()).append("$lt", filtro.getFim())));
        } else {
            matchDate.put("$match", new BasicDBObject("applicationDate", new BasicDBObject("$gt", filtro.getInicio()).append("$lt", filtro.getFim())));
        }

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

        DBObject limit2 = new BasicDBObject("$limit", limit);

        return ds.getCollection(Patent.class).aggregate(matchProj, matchComplete, matchDate,
                matchMainClassificationExists, matchBlacklist, group, sort, limit2);

    }

    private AggregationOutput getGroup(Project currentProject, int limit, Filters filtro) {
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

        DBObject matchComplete = new BasicDBObject();
        matchComplete.put("$match", new BasicDBObject("completed", filtro.isComplete()));

        DBObject matchDate = new BasicDBObject();
        if (filtro.getSelecionaData() == 1) {
            matchDate.put("$match", new BasicDBObject("publicationDate", new BasicDBObject("$gt", filtro.getInicio()).append("$lt", filtro.getFim())));
        } else {
            matchDate.put("$match", new BasicDBObject("applicationDate", new BasicDBObject("$gt", filtro.getInicio()).append("$lt", filtro.getFim())));
        }

        DBObject matchBlacklist = new BasicDBObject();
        matchBlacklist.put("$match", new BasicDBObject("blacklisted", false));

        DBObject matchMainClassificationExists = new BasicDBObject("$match",
                new BasicDBObject("mainClassification", new BasicDBObject(
                "$exists", true)));

        Object[] list = {"$mainClassification.klass",
            "$mainClassification.group"};
        DBObject section = new BasicDBObject("group", new BasicDBObject(
                "$concat", list));
        DBObject project = new BasicDBObject("$project", section);

        DBObject fields = new BasicDBObject("_id", "$group");
        fields.put("applicationPerSector", new BasicDBObject("$sum", 1));
        DBObject group = new BasicDBObject("$group", fields);

        DBObject sort = new BasicDBObject("$sort", new BasicDBObject(
                "applicationPerSector", -1));

        DBObject limit2 = new BasicDBObject("$limit", limit);

        return ds.getCollection(Patent.class).aggregate(matchProj, matchComplete, matchDate,
                matchMainClassificationExists, matchBlacklist, project, group,
                sort, limit2);

    }

    private AggregationOutput getSubGroup(Project currentProject, int limit, Filters filtro) {

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

        DBObject matchComplete = new BasicDBObject();
        matchComplete.put("$match", new BasicDBObject("completed", filtro.isComplete()));

        DBObject matchDate = new BasicDBObject();
        if (filtro.getSelecionaData() == 1) {
            matchDate.put("$match", new BasicDBObject("publicationDate", new BasicDBObject("$gt", filtro.getInicio()).append("$lt", filtro.getFim())));
        } else {
            matchDate.put("$match", new BasicDBObject("applicationDate", new BasicDBObject("$gt", filtro.getInicio()).append("$lt", filtro.getFim())));
        }

        DBObject matchBlacklist = new BasicDBObject();
        matchBlacklist.put("$match", new BasicDBObject("blacklisted", false));

        DBObject matchMainClassificationExists = new BasicDBObject("$match",
                new BasicDBObject("mainClassification", new BasicDBObject(
                "$exists", true)));

        Object[] list = {"$mainClassification.klass",
            "$mainClassification.group", "/",
            "$mainClassification.subgroup"};
        DBObject section = new BasicDBObject("group", new BasicDBObject(
                "$concat", list));
        DBObject project = new BasicDBObject("$project", section);

        DBObject fields = new BasicDBObject("_id", "$group");
        fields.put("applicationPerSector", new BasicDBObject("$sum", 1));
        DBObject group = new BasicDBObject("$group", fields);

        DBObject sort = new BasicDBObject("$sort", new BasicDBObject(
                "applicationPerSector", -1));

        DBObject limit2 = new BasicDBObject("$limit", limit);

        return ds.getCollection(Patent.class).aggregate(matchProj, matchComplete, matchDate,
                matchMainClassificationExists, matchBlacklist, project, group,
                sort, limit2);

    }
}
