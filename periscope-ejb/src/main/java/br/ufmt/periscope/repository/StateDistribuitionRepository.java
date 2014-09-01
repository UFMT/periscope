package br.ufmt.periscope.repository;

import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.report.Pair;
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
public class StateDistribuitionRepository {

    private @Inject
    Datastore ds;

    public List<Pair> getStateDistribuitions(Project currentProject) {

//        db.Patent.aggregate({$unwind:"$applicants"},
//        {$project:{applicants:1}},
//        {$match:{"applicants.country.acronym":"BR"}}, {$project:{"applicants.state":1}}
//        ,{$group:{_id:{_id:"$_id",state:"$applicants.state"}}},{$group:{_id:"$_id.state.acronym",count:{$sum:1}}},{$sort : {count: -1}})
        System.out.println("entrou repositorio");
        ArrayList<DBObject> parametros = new ArrayList<DBObject>();
        DBObject matchProj = new BasicDBObject("$match", new BasicDBObject("project.$id", currentProject.getId()));

        DBObject unwind = new BasicDBObject("$unwind", "$applicants");
        parametros.add(unwind);

        DBObject project = new BasicDBObject("$project", new BasicDBObject("applicants", 1));
        parametros.add(project);

        DBObject fields = new BasicDBObject("applicants.country.acronym", "BR");
        DBObject match = new BasicDBObject("$match", fields);
        parametros.add(match);

        DBObject project2 = new BasicDBObject("$project", new BasicDBObject("applicants.state", 1));
        parametros.add(project2);

        DBObject id = new BasicDBObject("_id", "$_id");
        id.put("state", "$applicants.state");
        DBObject _id = new BasicDBObject("_id", id);

        DBObject group = new BasicDBObject("$group", _id);
        parametros.add(group);

        DBObject id2 = new BasicDBObject("_id", "$_id.state.acronym");
        id2.put("count", new BasicDBObject("$sum", 1));

        DBObject group2 = new BasicDBObject("$group", id2);
        parametros.add(group2);

        DBObject sort = new BasicDBObject("$sort", new BasicDBObject("count", -1));
        parametros.add(sort);

        DBObject[] parameters = new DBObject[parametros.size()];
        parameters = parametros.toArray(parameters);

        AggregationOutput output = ds.getCollection(Patent.class).aggregate(matchProj, parameters);

        BasicDBList outputResult = (BasicDBList) output.getCommandResult().get("result");

        List<Pair> pairs = new ArrayList<Pair>();
        for (Object object : outputResult) {
            DBObject aux = (DBObject) object;
            if (aux.get("_id") != null && aux.get("_id") != "") {
                String state = aux.get("_id").toString();
                Integer count = (Integer) aux.get("count");

                pairs.add(new Pair(state, count));
            }

        }
        System.out.println("saiu repositorio");
        return pairs;

    }
}
