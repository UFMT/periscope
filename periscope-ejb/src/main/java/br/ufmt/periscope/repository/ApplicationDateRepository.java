package br.ufmt.periscope.repository;

import java.util.ArrayList;
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
public class ApplicationDateRepository {

    private @Inject
    Datastore ds;

    public List<Pair> getApplicationsByDate(Project projetoAtual, Filters filtro) {

        /**
         * db.Patent.aggregate( {$match:{"project.$id":new
         * ObjectId("51db042d44ae70d2d3649c20")}}, {$match:{blacklisted:false}},
         * {$project:{ year1:{$year:"$applicationDate"}}},
         * {$group:{_id:"$year1",ApplicationPerYear:{$sum:1}}}, {$sort:{_id:1}}
         * );
         */
        MongoCollection<Document> coll = ds.getDatabase()
                .getCollection(ds.getMapper().getEntityModel(Patent.class).getCollectionName());

        List<Document> pipeline = new ArrayList<Document>();
        pipeline.add(new Document("$match", new Document("project.$id", projetoAtual.getId())));

        if (filtro.isComplete()) {
            pipeline.add(new Document("$match", new Document("completed", filtro.isComplete())));
        }

        if (filtro.getInicio() != null && filtro.getFim() != null) {
            pipeline.add(new Document("$match", new Document("applicationDate",
                    new Document("$gte", filtro.getInicio()).append("$lte", filtro.getFim()))));
        }

        pipeline.add(new Document("$match", new Document("blacklisted", false)));

        pipeline.add(new Document("$project", new Document("year1",
                new Document("$year", "$applicationDate"))));

        pipeline.add(new Document("$group", new Document("_id", "$year1")
                .append("applicationPerYear", new Document("$sum", 1))));

        pipeline.add(new Document("$sort", new Document("_id", 1)));

        List<Document> outputResult = coll.aggregate(pipeline).into(new ArrayList<Document>());

        List<Pair> pairs = new ArrayList<Pair>();
        for (Document aux : outputResult) {
            Object id = aux.get("_id");
            if (id == null) {
                continue;
            }
            String year = id.toString();
            Number count = (Number) aux.get("applicationPerYear");
            pairs.add(new Pair(year, count));
        }
        return pairs;
    }
}
