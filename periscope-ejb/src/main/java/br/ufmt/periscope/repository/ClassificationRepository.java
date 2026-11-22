package br.ufmt.periscope.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.report.Pair;
import br.ufmt.periscope.util.Filters;

import com.mongodb.client.MongoCollection;
import dev.morphia.Datastore;
import org.bson.Document;

@ApplicationScoped
@Named
public class ClassificationRepository {

    private @Inject
    Datastore ds;

    public List<Pair> getMainIPC(Project currentProject, boolean klass,
            boolean subKlass, boolean group, boolean subGroup, int limit, Filters filtro, int classification) {

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
        Document fields = null;

        List<Document> pipeline = new ArrayList<Document>();

        Document matchParameters = new Document();

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

        if (filtro.getInicio() != null && filtro.getFim() != null) {
            if (filtro.getSelecionaData() == 1) {
                matchParameters.put("publicationDate",
                        new Document("$gte", filtro.getInicio()).append("$lte", filtro.getFim()));
            } else {
                matchParameters.put("applicationDate",
                        new Document("$gte", filtro.getInicio()).append("$lte", filtro.getFim()));
            }
        }

        matchParameters.put("blacklisted", false);

        if (classification == 2) {
            matchParameters.put("mainCPCClassification", new Document("$exists", true));
        } else {
            matchParameters.put("mainClassification", new Document("$exists", true));
        }

        pipeline.add(new Document("$match", matchParameters));

        if (!klass) {
            // classe nao esta selecionada
            // buscar secao
            fields = getSection(pipeline, classification);
            subKlass = false;
            group = false;
            subGroup = false;
        } else if (!subKlass) {
            // classe selecionada e subclasse nao esta
            // buscar classe
            fields = getKlass(pipeline, classification);
            group = false;
            subGroup = false;
        } else if (!group) {
            // classe e subclasse selecionadas e grupo nao selecionado
            // buscar subclasse
            fields = getSubKlass(pipeline, classification);
            subGroup = false;
        } else if (!subGroup) {
            // classe, subclasse e grupo selecionado, subgrupo nao selecioando
            // buscar grupo
            fields = getGroup(pipeline, classification);
        } else {
            // tudo selecionado
            // buscar subgrupo
            fields = getSubGroup(pipeline, classification);
        }

        pipeline.add(new Document("$group", fields));
        pipeline.add(new Document("$sort", new Document("applicationPerSector", -1)));
        pipeline.add(new Document("$limit", limit));

        MongoCollection<Document> coll = ds.getDatabase()
                .getCollection(ds.getMapper().getEntityModel(Patent.class).getCollectionName());

        System.out.println("Comando Principais Classificações: " + pipeline);
        List<Document> outputResult = coll.aggregate(pipeline).into(new ArrayList<Document>());

//        db.Patent.aggregate({"$unwind" : "$applicants"},{"$match" : {"applicants.name":"PROCTER & GAMBLE"}},
//        { "$match" : { "blacklisted" : false}} , { "$match" : { "mainClassification" : { "$exists" : true}}} , 
//        { "$project" : { "section" : { "$substr" : [ "$mainClassification.value" , 0 , 1]}}} , 
//        { "$group" : { "_id" : "$section" , "applicationPerSector" : { "$sum" : 1}}} , { "$sort" : { "applicationPerSector" : -1}} ,
//        { "$limit" : 8})
        List<Pair> pairs = new ArrayList<Pair>();
        for (Document aux : outputResult) {
            Object id = aux.get("_id");
            if (id == null) {
                continue;
            }
            String ipc = id.toString();
            Number count = (Number) aux.get("applicationPerSector");
            pairs.add(new Pair(ipc, count));
        }
        return pairs;
    }

    private Document getSection(List<Document> pipeline, int classification) {
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
        if (classification == 2) {
            List<Object> list = Arrays.<Object>asList("$mainCPCClassification.value", 0, 1);
            Document section = new Document("section", new Document("$substr", list));
            pipeline.add(new Document("$project", section));

            return new Document("_id", "$section")
                    .append("applicationPerSector", new Document("$sum", 1));
        } else {
            List<Object> list = Arrays.<Object>asList("$mainClassification.value", 0, 1);
            Document section = new Document("section", new Document("$substr", list));
            pipeline.add(new Document("$project", section));

            return new Document("_id", "$section")
                    .append("applicationPerSector", new Document("$sum", 1));
        }

    }

    private Document getKlass(List<Document> pipeline, int classification) {
        /**
         * db.Patent.aggregate( {$match:{"project.$id":new
         * ObjectId("51db042d44ae70d2d3649c20")}},
         * {$match:{mainClassification:{$exists:true}}},
         * {$match:{blacklisted:false}},
         * {$project:{section:{$substr:["$mainClassification.klass",0,3]}}},
         * {$group:{_id:"$section",applicationPerSector:{$sum:1}}},
         * {$sort:{applicationPerSector:-1}} );
         */

        if (classification == 2) {
            List<Object> list = Arrays.<Object>asList("$mainCPCClassification.klass", 0, 3);
            Document section = new Document("section", new Document("$substr", list));
            pipeline.add(new Document("$project", section));

            return new Document("_id", "$section")
                    .append("applicationPerSector", new Document("$sum", 1));
        } else {
            List<Object> list = Arrays.<Object>asList("$mainClassification.klass", 0, 3);
            Document section = new Document("section", new Document("$substr", list));
            pipeline.add(new Document("$project", section));

            return new Document("_id", "$section")
                    .append("applicationPerSector", new Document("$sum", 1));
        }
    }

    private Document getSubKlass(List<Document> pipeline, int classification) {
        /**
         * db.Patent.aggregate( {$match:{"project.$id":new
         * ObjectId("51db042d44ae70d2d3649c20")}},
         * {$match:{mainClassification:{$exists:true}}},
         * {$match:{blacklisted:false}},
         * {$group:{_id:"$mainClassification.klass"
         * ,applicationPerSector:{$sum:1}}}, {$sort:{applicationPerSector:-1}}
         * );
         */

        if (classification == 2) {
            return new Document("_id", "$mainCPCClassification.klass")
                    .append("applicationPerSector", new Document("$sum", 1));
        } else {
            return new Document("_id", "$mainClassification.klass")
                    .append("applicationPerSector", new Document("$sum", 1));
        }

    }

    private Document getGroup(List<Document> pipeline, int classification) {
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

        if (classification == 2) {
            List<Object> list = Arrays.<Object>asList("$mainCPCClassification.klass",
                    "$mainCPCClassification.group");
            Document section = new Document("group", new Document("$concat", list));
            pipeline.add(new Document("$project", section));

            return new Document("_id", "$group")
                    .append("applicationPerSector", new Document("$sum", 1));
        } else {
            List<Object> list = Arrays.<Object>asList("$mainClassification.klass",
                    "$mainClassification.group");
            Document section = new Document("group", new Document("$concat", list));
            pipeline.add(new Document("$project", section));

            return new Document("_id", "$group")
                    .append("applicationPerSector", new Document("$sum", 1));
        }

    }

    private Document getSubGroup(List<Document> pipeline, int classification) {

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
        if (classification == 2) {
            List<Object> list = Arrays.<Object>asList("$mainCPCClassification.klass",
                    "$mainCPCClassification.group", "/",
                    "$mainCPCClassification.subgroup");
            Document section = new Document("group", new Document("$concat", list));
            pipeline.add(new Document("$project", section));

            return new Document("_id", "$group")
                    .append("applicationPerSector", new Document("$sum", 1));
        } else {
            List<Object> list = Arrays.<Object>asList("$mainClassification.klass",
                    "$mainClassification.group", "/",
                    "$mainClassification.subgroup");
            Document section = new Document("group", new Document("$concat", list));
            pipeline.add(new Document("$project", section));

            return new Document("_id", "$group")
                    .append("applicationPerSector", new Document("$sum", 1));
        }

    }
}
