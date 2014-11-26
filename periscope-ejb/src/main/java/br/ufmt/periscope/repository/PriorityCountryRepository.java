package br.ufmt.periscope.repository;

import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.report.Pair;
import br.ufmt.periscope.util.Filters;
import com.github.jmkgreen.morphia.Datastore;
import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;

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
        ArrayList<DBObject> parametros = new ArrayList<DBObject>();

        DBObject matchProj = new BasicDBObject();
        matchProj.put("$match", new BasicDBObject("project.$id", currentProject.getId()));

        if (filtro.isComplete()) {
            DBObject matchComplete = new BasicDBObject();
            matchComplete.put("$match", new BasicDBObject("completed", filtro.isComplete()));
            parametros.add(matchComplete);
        }

        DBObject matchDate = new BasicDBObject();
        if (filtro.getSelecionaData() == 1) {
            matchDate.put("$match", new BasicDBObject("publicationDate", new BasicDBObject("$gte", filtro.getInicio()).append("$lte", filtro.getFim())));
        } else {
            matchDate.put("$match", new BasicDBObject("applicationDate", new BasicDBObject("$gte", filtro.getInicio()).append("$lte", filtro.getFim())));
        }
        parametros.add(matchDate);

        DBObject matchBlacklist = new BasicDBObject();
        matchBlacklist.put("$match", new BasicDBObject("blacklisted", false));
        parametros.add(matchBlacklist);

        DBObject unwind = new BasicDBObject("$unwind", "$priorities");
        parametros.add(unwind);

        DBObject sort1 = new BasicDBObject("$sort", new BasicDBObject("priorities.date", 1));
        parametros.add(sort1);

        DBObject group1 = new BasicDBObject();

        DBObject fields = new BasicDBObject("_id", "$_id");
        fields.put("country", new BasicDBObject("$first", "$priorities.country"));
        group1.put("$group", fields);
        parametros.add(group1);

        DBObject group2 = new BasicDBObject();
        parametros.add(group2);
        fields = new BasicDBObject("_id", "$country");
        fields.put("prioritiesPerCountry", new BasicDBObject("$sum", 1));
        group2.put("$group", fields);

        DBObject sort2 = new BasicDBObject("$sort", new BasicDBObject(
                "prioritiesPerCountry", -1));
        parametros.add(sort2);

        DBObject pipeLimit = new BasicDBObject("$limit", limit);
        parametros.add(pipeLimit);

        DBObject[] parameters = new DBObject[parametros.size()];
        parameters = parametros.toArray(parameters);

        AggregationOutput output = ds.getCollection(Patent.class).aggregate(matchProj, parameters);

        BasicDBList outputResult = (BasicDBList) output.getCommandResult().get(
                "result");

        List<Pair> pairs = new ArrayList<Pair>();
        for (Object object : outputResult) {
            DBObject aux = (DBObject) object;
            DBObject countryName = (DBObject) aux.get("_id");
            String country = "Without Priority Country";
            if (countryName != null) {

                country = countryName.get("name").toString();

            }
            Integer count = (Integer) aux.get("prioritiesPerCountry");
            pairs.add(new Pair(country, count));
        }
        return pairs;
    }
}
