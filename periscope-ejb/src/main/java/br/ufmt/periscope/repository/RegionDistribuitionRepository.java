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
public class RegionDistribuitionRepository {

    private @Inject
    Datastore ds;

    public List<Pair> getRegionDistribuitions(Project currentProject) {
        return getRegionDistribuitions(currentProject, null);
    }

    public List<Pair> getRegionDistribuitions(Project currentProject, Filters filtro) {
        MongoCollection<Document> coll = ds.getDatabase()
                .getCollection(ds.getMapper().getEntityModel(Patent.class).getCollectionName());

        List<Document> pipeline = new ArrayList<Document>();
        Document match = new Document("project.$id", currentProject.getId())
                .append("blacklisted", false);
        if (filtro != null) {
            if (filtro.isComplete()) {
                match.append("completed", true);
            }
            if (filtro.getInicio() != null && filtro.getFim() != null) {
                String dateField = filtro.getSelecionaData() == 1 ? "publicationDate" : "applicationDate";
                match.append(dateField,
                        new Document("$gte", filtro.getInicio()).append("$lte", filtro.getFim()));
            }
        }
        pipeline.add(new Document("$match", match));
        pipeline.add(new Document("$unwind", "$applicants"));
        pipeline.add(new Document("$project", new Document("applicants", 1)));
        pipeline.add(new Document("$match", new Document("applicants.country.acronym", "BR")));
        pipeline.add(new Document("$project", new Document("applicants.state", 1)));

        Document id = new Document("_id", "$_id").append("state", "$applicants.state");
        pipeline.add(new Document("$group", new Document("_id", id)));

        pipeline.add(new Document("$group", new Document("_id", "$_id.state.region")
                .append("count", new Document("$sum", 1))));

        pipeline.add(new Document("$sort", new Document("count", -1)));

        List<Document> outputResult = coll.aggregate(pipeline).into(new ArrayList<Document>());

        List<Pair> pairs = new ArrayList<Pair>();
        for (Document aux : outputResult) {
            if (aux.get("_id") != null && !"".equals(aux.get("_id"))) {
                String state = aux.get("_id").toString();
                Number count = (Number) aux.get("count");
                pairs.add(new Pair(state, count));
            }
        }
        return pairs;

    }
}
