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

        /**
         * EXEMPLO DA CONSULTA db.Patent.aggregate({ "$match" : {
         * "project.$id":ObjectId("537e0955e4b0cfbec0f0dc96") ,
         * "applicants.name" : "SAUDI ARABIAN OIL CO" , "blacklisted" : false ,
         * "mainClassification" : { "$exists" : true}}} , { "$project" : {
         * "group" : { "$concat" : [ "$mainClassification.klass" ,
         * "$mainClassification.group" , "/" ,
         * "$mainClassification.subgroup"]}}} , { "$group" : { "_id" : "$group"
         * , "applicationPerSector" : { "$sum" : 1}}} , { "$sort" : {
         * "applicationPerSector" : -1}} , { "$limit" : 8})
         *
         */
        AggregationOutput output;
        DBObject fields = null;

        ArrayList<DBObject> parametros = new ArrayList<DBObject>();

        DBObject matchParameters = new BasicDBObject();

        matchParameters.put("project.$id", currentProject.getId());

        if (filtro.getApplicantName() != null && !filtro.getApplicantName().isEmpty()) {
            matchParameters.put("applicants.name", filtro.getApplicantName());
        }

        if (filtro.getInventorName() != null && !filtro.getInventorName().isEmpty()) {
            matchParameters.put("inventors.name", filtro.getInventorName());
        }

        if (filtro.isComplete()) {
            matchParameters.put("completed", filtro.isComplete());
        }

        if (filtro.getSelecionaData() == 0) {
            matchParameters.put("publicationDate", new BasicDBObject("$gte", filtro.getInicio()).append("$lte", filtro.getFim()));
        } else {
            matchParameters.put("applicationDate", new BasicDBObject("$gte", filtro.getInicio()).append("$lte", filtro.getFim()));
        }

        matchParameters.put("blacklisted", false);

        matchParameters.put("mainClassification", new BasicDBObject("$exists", true));

        if (!klass) {
            // classe nao esta selecionada
            // buscar secao
            fields = getSection(parametros);
            subKlass = false;
            group = false;
            subGroup = false;
        } else if (!subKlass) {
            // classe selecionada e subclasse nao esta
            // buscar classe
            fields = getKlass(parametros);
            group = false;
            subGroup = false;
        } else if (!group) {
            // classe e subclasse selecionadas e grupo nao selecionado
            // buscar subclasse
            fields = getSubKlass(parametros);
            subGroup = false;
        } else if (!subGroup) {
            // classe, subclasse e grupo selecionado, subgrupo nao selecioando
            // buscar grupo
            fields = getGroup(parametros);
        } else {
            // tudo selecionado
            // buscar subgrupo
            fields = getSubGroup(parametros);
        }

        DBObject groupDb = new BasicDBObject("$group", fields);
        parametros.add(groupDb);
        DBObject sort = new BasicDBObject("$sort", new BasicDBObject(
                "applicationPerSector", -1));
        parametros.add(sort);
        DBObject limit2 = new BasicDBObject("$limit", limit);
        parametros.add(limit2);

        DBObject[] parameters = new DBObject[parametros.size()];
        parameters = parametros.toArray(parameters);

        DBObject match = new BasicDBObject();
        match.put("$match", matchParameters);

        output = ds.getCollection(Patent.class).aggregate(match, parameters);

        BasicDBList outputResult = (BasicDBList) output.getCommandResult().get(
                "result");
//        System.out.println("Comando:" + output.getCommand().toString());

//        db.Patent.aggregate({"$unwind" : "$applicants"},{"$match" : {"applicants.name":"PROCTER & GAMBLE"}},
//        { "$match" : { "blacklisted" : false}} , { "$match" : { "mainClassification" : { "$exists" : true}}} , 
//        { "$project" : { "section" : { "$substr" : [ "$mainClassification.value" , 0 , 1]}}} , 
//        { "$group" : { "_id" : "$section" , "applicationPerSector" : { "$sum" : 1}}} , { "$sort" : { "applicationPerSector" : -1}} ,
//        { "$limit" : 8})
        List<Pair> pairs = new ArrayList<Pair>();
        for (Object object : outputResult) {
            DBObject aux = (DBObject) object;
            String ipc = aux.get("_id").toString();
            Integer count = (Integer) aux.get("applicationPerSector");

            pairs.add(new Pair(ipc, count));
        }
        return pairs;
    }

    private DBObject getSection(ArrayList<DBObject> parametros) {
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
        Object[] list = new Object[]{"$mainClassification.value", 0, 1};
        DBObject section = new BasicDBObject("section", new BasicDBObject(
                "$substr", list));

        DBObject project = new BasicDBObject("$project", section);
        parametros.add(project);

        DBObject fields = new BasicDBObject("_id", "$section");
        fields.put("applicationPerSector", new BasicDBObject("$sum", 1));
        return fields;

    }

    private DBObject getKlass(ArrayList<DBObject> parametros) {
        /**
         * db.Patent.aggregate( {$match:{"project.$id":new
         * ObjectId("51db042d44ae70d2d3649c20")}},
         * {$match:{mainClassification:{$exists:true}}},
         * {$match:{blacklisted:false}},
         * {$project:{section:{$substr:["$mainClassification.klass",0,3]}}},
         * {$group:{_id:"$section",applicationPerSector:{$sum:1}}},
         * {$sort:{applicationPerSector:-1}} );
         */
        Object[] list = new Object[]{"$mainClassification.klass", 0, 3};
        DBObject section = new BasicDBObject("section", new BasicDBObject(
                "$substr", list));

        DBObject project = new BasicDBObject("$project", section);
        parametros.add(project);
        DBObject fields = new BasicDBObject("_id", "$section");
        fields.put("applicationPerSector", new BasicDBObject("$sum", 1));

        return fields;
    }

    private DBObject getSubKlass(ArrayList<DBObject> parametros) {
        /**
         * db.Patent.aggregate( {$match:{"project.$id":new
         * ObjectId("51db042d44ae70d2d3649c20")}},
         * {$match:{mainClassification:{$exists:true}}},
         * {$match:{blacklisted:false}},
         * {$group:{_id:"$mainClassification.klass"
         * ,applicationPerSector:{$sum:1}}}, {$sort:{applicationPerSector:-1}}
         * );
         */
        DBObject fields = new BasicDBObject("_id", "$mainClassification.klass");
        fields.put("applicationPerSector", new BasicDBObject("$sum", 1));

        return fields;

    }

    private DBObject getGroup(ArrayList<DBObject> parametros) {
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
        Object[] list = {"$mainClassification.klass",
            "$mainClassification.group"};
        DBObject section = new BasicDBObject("group", new BasicDBObject(
                "$concat", list));
        DBObject project = new BasicDBObject("$project", section);
        parametros.add(project);

        DBObject fields = new BasicDBObject("_id", "$group");
        fields.put("applicationPerSector", new BasicDBObject("$sum", 1));
        return fields;

    }

    private DBObject getSubGroup(ArrayList<DBObject> parametros) {

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
        Object[] list = {"$mainClassification.klass",
            "$mainClassification.group", "/",
            "$mainClassification.subgroup"};
        DBObject section = new BasicDBObject("group", new BasicDBObject(
                "$concat", list));
        DBObject project = new BasicDBObject("$project", section);
        parametros.add(project);

        DBObject fields = new BasicDBObject("_id", "$group");
        fields.put("applicationPerSector", new BasicDBObject("$sum", 1));
        return fields;

    }
}
