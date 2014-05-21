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
public class PriorityDateRepository {
    
    private @Inject Datastore ds;
    
    public List<Pair> getPrioritiesByDate(Project currentProject, Filters filtro){
        
        /* db.Patent.aggregate({$match:{"project.$id":new ObjectId("51db042d44ae70d2d3649c20")}} ,{$match:{blacklisted:false}},
        *   {$unwind:"$priorities"},{$sort:{"priorities.date":1}},
        *   {$project:{year1:{$year:"$priorities.date"}}}, 
        *   {$group:{_id:"$_id", year:{$first:"$year1"}}}, 
        *   {$group:{_id:"$year", prioritiesPerYear:{$sum:1}}});
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
        if (filtro.getSelecionaData() == 0) {
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
        
        DBObject project = new BasicDBObject();
        project.put("$project", new BasicDBObject("year1", new BasicDBObject(
                "$year", "$priorities.date")));
        parametros.add(project);
        
        DBObject group1 = new BasicDBObject();
        DBObject fields1 = new BasicDBObject();
        fields1.put("_id", "$_id");
        fields1.put("year", new BasicDBObject("$first", "$year1"));
        group1.put("$group", fields1);
        parametros.add(group1);
        
        DBObject group2 = new BasicDBObject();
        DBObject fields2 = new BasicDBObject();
        fields2.put("_id", "$year");
        fields2.put("prioritiesPerYear", new BasicDBObject("$sum", 1));
        group2.put("$group", fields2);
        parametros.add(group2);
        
        DBObject sort2 = new BasicDBObject("$sort", new BasicDBObject("_id", 1));
        parametros.add(sort2);
        
        DBObject[] parameters = new DBObject[parametros.size()];
        parameters = parametros.toArray(parameters);
        
        AggregationOutput output = ds.getCollection(Patent.class).aggregate(matchProj, parameters);
        
        BasicDBList outputResult = (BasicDBList) output.getCommandResult().get("result");
        
        List<Pair> pairs = new ArrayList<Pair>();
        for (Object object : outputResult) {
            DBObject aux = (DBObject) object;
            if (!aux.get("_id").toString().equals("-1")) {
                
                String year = aux.get("_id").toString();
                Integer count = (Integer) aux.get("prioritiesPerYear");

                pairs.add(new Pair(year, count));
            }
        }
        
        return pairs;
    }
}
