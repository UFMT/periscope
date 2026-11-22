package br.ufmt.periscope.repository;

import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.report.Pair;
import br.ufmt.periscope.util.Filters;
import com.mongodb.client.MongoCollection;
import dev.morphia.Datastore;
import java.util.ArrayList;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.bson.Document;

@ApplicationScoped
@Named
public class PriorityCountryRepository {

    private @Inject
    Datastore ds;

    public List<Pair> getPriorities(Project currentProject, int limit, Filters filtro) {
        /**
         * db.Patent.aggregate( {$match:{"project.$id":new
         * ObjectId("51db042d44ae70d2d3649c20")}} ,{$match:{blacklisted:false}},
         * {$unwind:"$priorities"} {$group:{_id:"$priorities.country",
         * prioritiesPerCountry:{$sum:1}}}, {$sort:{prioritiesPerCountry:-1}},
         * {$limit:5});
         */
        MongoCollection<Document> coll = ds.getDatabase()
                .getCollection(ds.getMapper().getEntityModel(Patent.class).getCollectionName());

        List<Document> pipeline = new ArrayList<Document>();

        pipeline.add(new Document("$match", new Document("project.$id", currentProject.getId())));

        if (filtro != null) {
            if (filtro.isComplete()) {
                pipeline.add(new Document("$match", new Document("completed", true)));
            }
            if (filtro.getInicio() != null && filtro.getFim() != null) {
                if (filtro.getSelecionaData() == 1) {
                    pipeline.add(new Document("$match", new Document("publicationDate",
                            new Document("$gte", filtro.getInicio()).append("$lte", filtro.getFim()))));
                } else {
                    pipeline.add(new Document("$match", new Document("applicationDate",
                            new Document("$gte", filtro.getInicio()).append("$lte", filtro.getFim()))));
                }
            }
        }

        pipeline.add(new Document("$match", new Document("blacklisted", false)));
        pipeline.add(new Document("$unwind", "$priorities"));
        pipeline.add(new Document("$sort", new Document("priorities.date", 1)));

        pipeline.add(new Document("$group", new Document("_id", "$_id")
                .append("country", new Document("$first", "$priorities.country"))));

        pipeline.add(new Document("$group", new Document("_id", "$country")
                .append("prioritiesPerCountry", new Document("$sum", 1))));

        pipeline.add(new Document("$sort", new Document("prioritiesPerCountry", -1)));
        pipeline.add(new Document("$limit", limit));

        List<Document> outputResult = coll.aggregate(pipeline).into(new ArrayList<Document>());

        List<Pair> pairs = new ArrayList<Pair>();
        for (Document aux : outputResult) {
            Document countryName = (Document) aux.get("_id");
            String country = "Without Priority Country";
            if (countryName != null) {
                country = countryName.get("name").toString();
            }
            Number count = (Number) aux.get("prioritiesPerCountry");
            pairs.add(new Pair(country, count));
        }
        return pairs;
    }
}
