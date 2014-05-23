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
public class NatureApplicantRepository {
    
    private @Inject Datastore ds;
    
    public List<Pair> getNatureApplicantRepository(Project currentProject, Filters filtro){
        
        //db.Patent.aggregate({$match:{blacklisted:false}},
        //{$unwind:"$applicants"},{$group:{_id:"$applicants.nature.name", count:{$sum:1}}})
        
        ArrayList<DBObject> parametros = new ArrayList<DBObject>();
        DBObject matchProj = new BasicDBObject();
        matchProj.put("$match", new BasicDBObject("project.$id", currentProject.getId()));
        
        if (filtro.isComplete()) {
            DBObject matchComplete = new BasicDBObject();
            matchComplete.put("$match", new BasicDBObject("completed", filtro.isComplete()));
            parametros.add(matchComplete);
        }
                
        DBObject matchBlacklist = new BasicDBObject();
        matchBlacklist.put("$match", new BasicDBObject("blacklisted", false));
        parametros.add(matchBlacklist);
        
        DBObject unwind = new BasicDBObject("$unwind", "$applicants");
        parametros.add(unwind);
        
        DBObject fields = new BasicDBObject();
        fields.put("_id", "$applicants.nature.name");
        fields.put("count", new BasicDBObject("$sum", 1));
        
        DBObject group = new BasicDBObject("$group", fields);
        parametros.add(group);
        
        DBObject sort = new BasicDBObject("$sort", new BasicDBObject("count",-1));
        parametros.add(sort);
        
        DBObject[] parameters = new DBObject[parametros.size()];
        parameters = parametros.toArray(parameters);
        
        AggregationOutput output = ds.getCollection(Patent.class).aggregate(matchProj, parameters);
        
        BasicDBList outputResult = (BasicDBList) output.getCommandResult().get("result");
        
        List<Pair> pairs = new ArrayList<Pair>();
        for (Object object : outputResult) {
            DBObject aux = (DBObject) object;
            if (aux.get("_id") != null) {
                
                String type = aux.get("_id").toString();
                Integer count = (Integer) aux.get("count");

                pairs.add(new Pair(type, count));
            }
        }
        
        return pairs;
    }
}
