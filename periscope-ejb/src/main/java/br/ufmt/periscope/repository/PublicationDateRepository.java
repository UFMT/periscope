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
public class PublicationDateRepository {

    private @Inject
    Datastore ds;

    public List<Pair> getPublicationsByDate(Project projetoAtual, Filters filtro) {

        /**
          db.Patent.aggregate({$match:{blacklisted:false}},
          {$project:{ year1:{$year:"$publicationDate"}}},
          {$group:{_id:"$year1",PublicationPerYear:{$sum:1}}}, {$sort:{_id:1}}
          );
         */
        ArrayList<DBObject> parametros = new ArrayList<DBObject>();
        DBObject matchProj = new BasicDBObject();
        matchProj.put("$match", new BasicDBObject("project.$id", projetoAtual.getId()));


        if (filtro.isComplete()) {
            DBObject matchComplete = new BasicDBObject();
            matchComplete.put("$match", new BasicDBObject("completed", filtro.isComplete()));
            parametros.add(matchComplete);
        }

        DBObject matchDate = new BasicDBObject();
        matchDate.put("$match", new BasicDBObject("publicationDate", new BasicDBObject("$gte", filtro.getInicio()).append("$lte", filtro.getFim())));

        parametros.add(matchDate);

        DBObject matchBlacklist = new BasicDBObject();
        matchBlacklist.put("$match", new BasicDBObject("blacklisted", false));
        parametros.add(matchBlacklist);

        DBObject project = new BasicDBObject();
        project.put("$project", new BasicDBObject("year1", new BasicDBObject(
                "$year", "$publicationDate")));
        parametros.add(project);

        DBObject group = new BasicDBObject();
        DBObject fields = new BasicDBObject("_id", "$year1");
        fields.put("publilicationPerYear", new BasicDBObject("$sum", 1));
        group.put("$group", fields);
        parametros.add(group);

        DBObject sort = new BasicDBObject("$sort", new BasicDBObject("_id", 1));
        parametros.add(sort);

        DBObject[] parameters = new DBObject[parametros.size()];
        parameters = parametros.toArray(parameters);

        AggregationOutput output = ds.getCollection(Patent.class).aggregate(
                matchProj, parameters);


        BasicDBList outputResult = (BasicDBList) output.getCommandResult().get("result");

        List<Pair> pairs = new ArrayList<Pair>();
        for (Object object : outputResult) {
            DBObject aux = (DBObject) object;
//            if (!aux.get("_id").toString().equals("-1")) {

            String year = aux.get("_id").toString();
            Integer count = (Integer) aux.get("publicationPerYear");

            pairs.add(new Pair(year, count));
//            }
        }
        return pairs;
    }
}
