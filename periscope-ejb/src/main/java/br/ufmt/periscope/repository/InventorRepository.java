package br.ufmt.periscope.repository;

import br.ufmt.periscope.indexer.resources.search.FuzzyTokenSimilaritySearch;
import br.ufmt.periscope.model.Country;
import br.ufmt.periscope.model.Inventor;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.State;
import br.ufmt.periscope.report.Pair;
import br.ufmt.periscope.util.Filters;
import com.github.jmkgreen.morphia.Datastore;
import com.github.jmkgreen.morphia.mapping.Mapper;
import com.github.jmkgreen.morphia.mapping.cache.EntityCache;
import com.google.common.collect.HashMultiset;
import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.lucene.document.Document;

/**
 * 
 * This class have the methods with the queries for Inventor
 */
@ViewScoped
@Named
public class InventorRepository {

    private @Inject
    Datastore ds;
    private @Inject
    Project currentProject;
    private int count;
    private Integer searchType;
    private @Inject
    FuzzyTokenSimilaritySearch fs;

    public Inventor getInventorByName(String name) {

        Inventor ret = null;
        BasicDBObject eleMatch = new BasicDBObject("$elemMatch", new BasicDBObject("name", name));
        BasicDBObject inventors = new BasicDBObject("inventors", eleMatch);
        BasicDBObject keys = new BasicDBObject("inventors", 1);
        DBCursor cursor = ds.getCollection(Patent.class).find(inventors, keys);
        int docCount = ds.getCollection(Patent.class).find(inventors, keys).count();
        if (cursor.hasNext()) {
            Mapper mapper = ds.getMapper();
            EntityCache ec = mapper.createEntityCache();

            BasicDBList objList = (BasicDBList) cursor.next().get("inventors");
            for (Object obj : objList) {
                ret = (Inventor) mapper.fromDBObject(Inventor.class, (DBObject) obj, ec);
                if (ret.getName().equals(name)) {
                    ret.setDocumentCount(docCount);
                    return ret;
                }
            }
        }
        return null;

    }
    /**
     * This method executes a query that's responsible to bring the MainInventor chart data.
     * @param currentProject Project - Project where the query must be executed.
     * @param limit int - Maximum amount of Inventors that should be bring.
     * @param filtro Filters - Filters to be applied in the query.
     * @return List&lt;Pair&gt; - List with the values that should be showed in the chart.
     */
    public List<Pair> updateInventors(Project currentProject, int limit, Filters filtro) {

        /**
         * db.Patent.aggregate( {$match:{"project.$id":new
         * ObjectId("51db042d44ae70d2d3649c20")}}, {$match:{blacklisted:false}},
         * {$unwind:"$inventors"},
         * {$group:{_id:"$inventors",applicationPerInventor:{$sum:1}}},
         * {$sort:{applicationPerInventor:-1}}, { $limit : 5 } );
         */
        ArrayList<DBObject> parametros = new ArrayList<DBObject>();

        DBObject matchProj = new BasicDBObject();
        matchProj.put("$match",
                new BasicDBObject("project.$id", currentProject.getId()));

        if (filtro.isComplete()) {
            DBObject matchComplete = new BasicDBObject();
            matchComplete.put("$match", new BasicDBObject("completed", filtro.isComplete()));
            parametros.add(matchComplete);
        }

        DBObject matchDate = new BasicDBObject();
//        System.out.println(filtro.getSelecionaData());
        if (filtro.getSelecionaData() == 1) {
            matchDate.put("$match", new BasicDBObject("publicationDate", new BasicDBObject("$gte", filtro.getInicio()).append("$lte", filtro.getFim())));
        } else {
            matchDate.put("$match", new BasicDBObject("applicationDate", new BasicDBObject("$gte", filtro.getInicio()).append("$lte", filtro.getFim())));
        }
        parametros.add(matchDate);

        DBObject matchBlacklist = new BasicDBObject();
        matchBlacklist.put("$match", new BasicDBObject("blacklisted", false));
        parametros.add(matchBlacklist);

        DBObject unwind = new BasicDBObject("$unwind", "$inventors");
        parametros.add(unwind);

        DBObject group = new BasicDBObject();
        parametros.add(group);
        DBObject fields = new BasicDBObject("_id", "$inventors");
        fields.put("applicationPerInventor", new BasicDBObject("$sum", 1));
        group.put("$group", fields);

        DBObject sort = new BasicDBObject("$sort", new BasicDBObject(
                "applicationPerInventor", -1));
        parametros.add(sort);

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
            DBObject inventorName = (DBObject) aux.get("_id");
            String inventor = inventorName.get("name").toString();
            Integer count = (Integer) aux.get("applicationPerInventor");

            pairs.add(new Pair(inventor, count));
        }
        return pairs;
    }

    /**
     * This methods gets a list of <b>Inventors</b> from database.
     * @param currentProject Project - Project where the query must be executed.
     * @return ArrayList&lt;Inventor&gt; List with the inventors of the Project.
     */
    public ArrayList<Inventor> getInventors(Project currentProject) {
        Map<String, Inventor> map = new HashMap<String, Inventor>();

        HashMultiset<String> bag = HashMultiset.create();

        BasicDBObject where = new BasicDBObject();
        where.put("project.$id", currentProject.getId());
        where.put("inventors", new BasicDBObject("$exists", true));

        BasicDBObject keys = new BasicDBObject();
        keys.put("inventors", 1);

        DBCursor cursor = ds.getCollection(Patent.class).find(where, keys);
        Mapper mapper = ds.getMapper();
        EntityCache ec = mapper.createEntityCache();
        while (cursor.hasNext()) {

            BasicDBList objList = (BasicDBList) cursor.next().get("inventors");
            Iterator<Object> itList = objList.iterator();
            while (itList.hasNext()) {
                Inventor pa = (Inventor) mapper.fromDBObject(Inventor.class, (DBObject) itList.next(), ec);
                bag.add(pa.getName());
                pa.setDocumentCount(bag.count(pa.getName()));
                map.put(pa.getName(), pa);
            }

        }
        ArrayList<Inventor> inventors = new ArrayList<Inventor>(map.values());
        Collections.sort(inventors);
        return inventors;
    }

   /**
    * This methods gets a list of <b>Inventors</b> from database.
    * @param project Project - Project where the query must be executed.
    * @param begins String - The inventor should begin with this String.
    * @return List&lt;String&gt; List with the inventors of the Project.
    */
    public List<String> getInventors(Project project, String begins) {
        ArrayList<DBObject> parametros = new ArrayList<DBObject>();
        DBObject matchProject = new BasicDBObject("$match", new BasicDBObject("project.$id", project.getId()).append("blacklisted", false));
        DBObject unwind = new BasicDBObject("$unwind", "$inventors");
        parametros.add(unwind);
        DBObject matchName = new BasicDBObject("$match", new BasicDBObject("inventors.name", new BasicDBObject("$regex", "^" + begins).append("$options", "i")));
        parametros.add(matchName);
        DBObject projection = new BasicDBObject("$project", new BasicDBObject("inventors", 1));
        parametros.add(projection);
        DBObject group = new BasicDBObject("$group", new BasicDBObject("_id", "$inventors.name"));
        parametros.add(group);
        DBObject parameters[] = new DBObject[parametros.size()];
        parameters = parametros.toArray(parameters);
        AggregationOutput output = ds.getCollection(Patent.class).aggregate(matchProject, parameters);
        BasicDBList outputList = (BasicDBList) output.getCommandResult().get("result");
        List<String> lista = new ArrayList<String>();
        for (Object inventor : outputList) {
            DBObject aux = (DBObject) inventor;
            String nome = aux.get("_id").toString();
            lista.add(nome);
        }
        return lista;

    }

    /**
     * 
     * 
     * @param project
     * @param top
     * @param names
     * @return 
     */
    public Set<String> getInventorSugestions(Project project, int top, String... names) {
        Set<String> results = new HashSet<String>();
        
        for (String name : names) {
            List<Document> docs = fs.search("inventor", project.getId().toString(), name, top);
            for (Document doc : docs)
                results.add(doc.get("inventor"));
        }
        
        return results;
    }

    public List<Inventor> load(int first, int pageSize, String sortField, int sortOrder, Map<String, String> filters, List<Inventor> list) {

        ArrayList<DBObject> parametros = new ArrayList<DBObject>();
        ArrayList<DBObject> parametrosGroup = new ArrayList<DBObject>();

        DBObject match = new BasicDBObject();

        DBObject matchProj = new BasicDBObject();
        matchProj.put("project.$id", currentProject.getId());
        matchProj.put("blacklisted", false);

        DBObject unwind = new BasicDBObject("$unwind", "$inventors");
        parametros.add(unwind);
        parametrosGroup.add(unwind);

        DBObject matchFilterItem = new BasicDBObject();
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            String column = entry.getKey();
            String value = entry.getValue();
            DBObject regex;
            if (searchType != null && searchType.equals(1)) {
                regex = new BasicDBObject("$regex", "^" + value).append("$options", "i");
            } else {
                regex = new BasicDBObject("$regex", value).append("$options", "i");
            }
            matchFilterItem.put("inventors." + column, regex);
        }

//        DBObject matchSearch = new BasicDBObject("$match", matchFilterItem);
//        parametros.add(matchSearch);
//        DBObject matchFilter = new BasicDBObject();
        if (list != null) {
            BasicDBList lista = new BasicDBList();
            List<String> t = new ArrayList<String>();
            for (Inventor inv : list) {
                t.add(inv.getName());
            }
            lista.addAll(t);
            matchFilterItem.put("inventors.name", new BasicDBObject("$nin", lista));
        }

        if (matchFilterItem.keySet().size() > 0) {
            DBObject matchEdit = new BasicDBObject("$match", matchFilterItem);
            parametros.add(matchEdit);
            parametrosGroup.add(matchEdit);
        }

        DBObject idData = new BasicDBObject("name", "$inventors.name");
        idData.put("country", "$inventors.country");
        idData.put("state", "$inventors.state");
        idData.put("acronym", "$inventors.acronym");
        idData.put("harmonized", "$inventors.harmonized");
        DBObject fields = new BasicDBObject("_id", idData);
        fields.put("documentCount", new BasicDBObject("$sum", 1));
        DBObject group = new BasicDBObject();
        group.put("$group", fields);
        parametros.add(group);
        parametrosGroup.add(group);

        fields = new BasicDBObject("_id", "nome");
        fields.put("documentCount", new BasicDBObject("$sum", 1));
        DBObject groupTotal = new BasicDBObject();
        groupTotal.put("$group", fields);
        parametrosGroup.add(groupTotal);

        if (sortField != null) {
            if ("documentCount".equals(sortField)) {
                DBObject sort = new BasicDBObject("$sort", new BasicDBObject(sortField, (sortOrder == 0 ? 1 : -1)));
                parametros.add(sort);
            } else {
                DBObject sort = new BasicDBObject("$sort", new BasicDBObject("_id." + sortField, (sortOrder == 0 ? 1 : -1)));
                parametros.add(sort);
            }
        } else {
            DBObject sort = new BasicDBObject("$sort", new BasicDBObject("_id.name", 1));
            parametros.add(sort);
        }

        DBObject skip = new BasicDBObject("$skip", first);
        parametros.add(skip);

        DBObject limit = new BasicDBObject("$limit", pageSize);
        parametros.add(limit);

        DBObject[] parameters = new DBObject[parametros.size()];
        parameters = parametros.toArray(parameters);

        DBObject[] parametersGroup = new DBObject[parametrosGroup.size()];
        parametersGroup = parametrosGroup.toArray(parametersGroup);

        match.put("$match", matchProj);
        AggregationOutput outputTotal = ds.getCollection(Patent.class).aggregate(match, parametersGroup);
        BasicDBList outputListTotal = (BasicDBList) outputTotal.getCommandResult().get("result");
        for (Object patent : outputListTotal) {
            DBObject result = (DBObject) patent;
            this.setCount(Integer.parseInt(result.get("documentCount").toString()));
            break;
        }
        AggregationOutput output = ds.getCollection(Patent.class).aggregate(match, parameters);

        BasicDBList outputList = (BasicDBList) output.getCommandResult().get("result");

//        this.setCount(output.);
        List<Inventor> datasource = new ArrayList<Inventor>();
        for (Object patent : outputList) {
            DBObject aux = (DBObject) patent;
            DBObject result = (DBObject) aux.get("_id");
            Inventor inventor = new Inventor();
            inventor.setName(result.get("name").toString());
            if (result.get("acronym") != null) {
                inventor.setAcronym(result.get("acronym").toString());
            }

            DBObject country = (DBObject) result.get("country");
            if (country != null) {

                Country realCountry = new Country();
                realCountry.setAcronym((String) country.get("acronym"));
                realCountry.setName((String) country.get("name"));
                inventor.setCountry(realCountry);
            } else {
                inventor.setCountry(null);
            }

            DBObject state = (DBObject) result.get("state");
            if (state != null) {
                State realState = new State();
                realState.setAcronym((String) state.get("acronym"));
                realState.setRegion((String) state.get("region"));
                realState.setName((String) state.get("name"));
                inventor.setState(realState);
            } else {
                inventor.setState(null);
            }
            inventor.setHarmonized((Boolean) result.get("harmonized"));
            inventor.setDocumentCount((Integer) aux.get("documentCount"));
            datasource.add(inventor);
        }

        return datasource;
    }

    public boolean exists(Inventor inventor) {

        ArrayList<DBObject> parametros = new ArrayList<DBObject>();

        DBObject matchProj = new BasicDBObject();
        matchProj.put("project.$id", currentProject.getId());
        matchProj.put("blacklisted", false);
        DBObject matchP = new BasicDBObject("$match", matchProj);

        DBObject unwind = new BasicDBObject("$unwind", "$inventors");
        parametros.add(unwind);

        DBObject fields = new BasicDBObject("inventors.country.acronym", inventor.getCountry().getAcronym());
        fields.put("inventors.name", inventor.getName());
        DBObject match = new BasicDBObject("$match", fields);
        parametros.add(match);

        DBObject idData = new BasicDBObject("name", "$inventors.name");
        DBObject field = new BasicDBObject("_id", idData);
        DBObject group = new BasicDBObject("$group", field);
        parametros.add(group);

        DBObject[] parameters = new DBObject[parametros.size()];
        parameters = parametros.toArray(parameters);

        AggregationOutput output = ds.getCollection(Patent.class).aggregate(matchP, parameters);

        BasicDBList outputList = (BasicDBList) output.getCommandResult().get("result");
        return outputList.size() == 0;

    }

    public List<Inventor> load(int first, int pageSize, String sortField, int sortOrder, Map<String, String> filters) {
        return load(first, pageSize, sortField, sortOrder, filters, null);
    }
    public int getCount() {
        return count;
    }
    public void setCount(int count) {
        this.count = count;
    }

    public Integer getSearchType() {
        return searchType;
    }

    public void setSearchType(Integer searchType) {
        this.searchType = searchType;
    }

    public Project getCurrentProject() {
        return currentProject;
    }

    public void setCurrentProject(Project currentProject) {
        this.currentProject = currentProject;
    }
}
