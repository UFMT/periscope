package br.ufmt.periscope.repository;

import br.ufmt.periscope.indexer.LuceneIndexerResources;
import br.ufmt.periscope.indexer.resources.analysis.FastJoinAnalyzer;
import br.ufmt.periscope.indexer.resources.search.FastJoinQuery;
import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.model.ApplicantType;
import br.ufmt.periscope.model.Country;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.State;
import br.ufmt.periscope.util.Filters;
import com.github.jmkgreen.morphia.Datastore;
import com.github.jmkgreen.morphia.mapping.Mapper;
import com.github.jmkgreen.morphia.mapping.cache.EntityCache;
import com.google.common.collect.HashMultiset;
import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MapReduceCommand;
import com.mongodb.MapReduceCommand.OutputType;
import java.io.IOException;
import java.io.StringReader;
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
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.util.Version;


@ViewScoped
/**
 * This class have the methods with the queries for Applicant
 */
@Named
public class ApplicantRepository {
    
    private @Inject
    Datastore ds;
    private @Inject
    LuceneIndexerResources resources;
    private IndexReader reader = null;
    private @Inject
    FastJoinAnalyzer analyzer;
    private @Inject
    Project currentProject;
    private int count;
    private List<Applicant> list;
    private Integer searchType;

    /**
     * Method that query an applicant by its name.
     * @param name String - Name of the applicant to be got.
     * @return Applicant 
     */
    public Applicant getApplicantByName(String name) {

        Applicant ret = null;
        BasicDBObject eleMatch = new BasicDBObject("$elemMatch", new BasicDBObject("name", name));
        BasicDBObject applicants = new BasicDBObject("applicants", eleMatch);
        BasicDBObject keys = new BasicDBObject("applicants", 1);
        DBCursor cursor = ds.getCollection(Patent.class).find(applicants, keys).limit(1);
        if (cursor.hasNext()) {
            Mapper mapper = ds.getMapper();
            EntityCache ec = mapper.createEntityCache();

            BasicDBList objList = (BasicDBList) cursor.next().get("applicants");
            Iterator<Object> itList = objList.iterator();
            if (itList.hasNext()) {
                ret = (Applicant) mapper.fromDBObject(Applicant.class, (DBObject) itList.next(), ec);
            }
            return ret;
        }
        return null;

    }

    /**
     * Method responsible to get the applicants from database.
     * @param project Project - Project in which the applicant most be searched.
     * @return List&lt;String&gt; - List with the applicants queried.
     */
    public List<Applicant> getApplicants(Project project) {
        Map<String, Applicant> map = new HashMap<String, Applicant>();

        HashMultiset<String> bag = HashMultiset.create();

        BasicDBObject where = new BasicDBObject();
        where.put("project.$id", project.getId());
        where.put("applicants", new BasicDBObject("$exists", true));

        BasicDBObject keys = new BasicDBObject();
        keys.put("applicants", 1);

        DBCursor cursor = ds.getCollection(Patent.class).find(where, keys).sort(new BasicDBObject("applicants.name", 1));
        Mapper mapper = ds.getMapper();
        EntityCache ec = mapper.createEntityCache();
        while (cursor.hasNext()) {

            BasicDBList objList = (BasicDBList) cursor.next().get("applicants");
            Iterator<Object> itList = objList.iterator();
            while (itList.hasNext()) {
                Applicant pa = (Applicant) mapper.fromDBObject(Applicant.class, (DBObject) itList.next(), ec);
                bag.add(pa.getName());
                pa.setDocumentCount(bag.count(pa.getName()));
                map.put(pa.getName(), pa);
            }

        }
        List<Applicant> ret = new ArrayList<Applicant>(map.values());
        Collections.sort(ret);
        return ret;
    }


    /**
     * Method responsible to get the applicants from database.
     * @param project Project - Project in which the applicant most be searched.
     * @param begins String - The applicant name must begins with this string
     * @return List&lt;String&gt; - List with the applicants queried.
     */
    public List<String> getApplicants(Project project,String begins) {
        ArrayList<DBObject> parametros = new ArrayList<DBObject>();
        DBObject matchProject = new BasicDBObject("$match", new BasicDBObject("project.$id", project.getId()).append("blacklisted", false));
        DBObject unwind = new BasicDBObject("$unwind", "$applicants");
        parametros.add(unwind);
        DBObject matchName = new BasicDBObject("$match", new BasicDBObject("applicants.name", new BasicDBObject("$regex", "^" + begins).append("$options", "i")));
        parametros.add(matchName);
        DBObject projection = new BasicDBObject("$project", new BasicDBObject("applicants", 1));
        parametros.add(projection);
        DBObject group = new BasicDBObject("$group", new BasicDBObject("_id", "$applicants.name"));
        parametros.add(group);
        DBObject parameters[] = new DBObject[parametros.size()];
        parameters = parametros.toArray(parameters);
        AggregationOutput output = ds.getCollection(Patent.class).aggregate(matchProject, parameters);
        BasicDBList outputList = (BasicDBList) output.getCommandResult().get("result");
        List<String> lista = new ArrayList<String>();
        for (Object applicant : outputList) {
            DBObject aux = (DBObject) applicant;
            String nome = aux.get("_id").toString();
            lista.add(nome);
        }
        return lista;

    }

    /**
     * Method responsible to update the mainApplicant collection with MapReduce.
     * @param currentProject Project - Current Project.
     * @param filtro Filters - filters to be applied in the query.
     */
    public void updateMainApplicants(Project currentProject, Filters filtro) {

        String map = "function() { "
                + "for(var i in this.applicants){ "
                + "emit(this.applicants[i].name,1); "
                + "}"
                + "}";
        String reduce = "function(name,values) { "
                + "total=0;"
                + "for(var i in values){ "
                + "total+=values[i]; "
                + "}"
                + "return total;"
                + "}";
        BasicDBObject where = new BasicDBObject();
        where.put("project.$id", currentProject.getId());
        where.put("applicants", new BasicDBObject("$exists", true));
        if (filtro.isComplete()) {
            where.put("completed", filtro.isComplete());
        }
        if (filtro.getSelecionaData() == 0) {
            where.put("publicationDate", new BasicDBObject("$gte", filtro.getInicio()).append("$lte", filtro.getFim()));
        } else {
            where.put("applicationDate", new BasicDBObject("$gte", filtro.getInicio()).append("$lte", filtro.getFim()));
        }
        if (filtro.getApplicantType() != null && !filtro.getApplicantType().isEmpty()) {
            where.put("applicants.nature.name", filtro.getApplicantType());

        }

        DBCollection coll = ds.getCollection(Patent.class);
        MapReduceCommand cmd = new MapReduceCommand(coll,
                map,
                reduce,
                "mainApplicant",
                OutputType.REPLACE,
                where);
        coll.mapReduce(cmd);

    }

    /**
     * Method responsible for searching with <i>LUCENE</i> the suggestions of Applicants for the harmonization.
     * @param project Project - Project where the suggestions should be searched.
     * @param top int - Maximum amount of names to be returned.
     * @param names String - Names of applicants to be queried.
     * @return Set&lt;String&gt; - Set of applicants names suggested by the query
     */
    public Set<String> getApplicantSugestions(Project project, int top, String... names) {
        Set<String> results = new HashSet<String>();
        reader = resources.getReader();
        try {
            Query queryProject = new QueryParser(Version.LUCENE_47, "project", analyzer)
                    .parse(project.getId().toString());
            queryProject.setBoost(0.1f);
            IndexSearcher searcher = new IndexSearcher(reader);
            for (String name : names) {
                TokenStream stream = analyzer.tokenStream("applicant", new StringReader(
                        name));
                CharTermAttribute attr = stream
                        .getAttribute(CharTermAttribute.class);
                stream.reset();
                String valor = "";
                while (stream.incrementToken()) {
                    valor = valor + attr.toString() + ' ';
                }
                valor = valor.trim();
                stream.end();
                stream.close();
                Query query = new FastJoinQuery("applicant", valor, 0.6f, 0.6f);
                ScoreDoc[] hits = searcher.search(query, top).scoreDocs;
                if (hits.length > 0) {
                    for (int i = 0; i < hits.length; i++) {
                        Document hitDoc = searcher.doc(hits[i].doc);
                        results.add(hitDoc.get("applicant"));
                    }
                }
            }
            for (String name : names) {
                results.remove(name);
            }

        } catch (CorruptIndexException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        resources.closeReader(reader);
        return results;

    }

    /**
     * Method responsible to search the data to fill the LazyTable of <b>Applicants</b>
     * @param first int - The offset of the query.
     * @param pageSize int - The limit of applicant in each page.
     * @param sortField String - Name of the column to be sorted.
     * @param sortOrder int - The Sort order of the column.
     * @param filters Map&lt;String, String&gt; - Map with the column and values of the filters in the table.
     * @param list List&lt;Applicant&gt; - List with the applicants that should not be queried.
     * @return List&lt;Applicant&gt; - List of the applicants to be put in the table.
     */
    public List<Applicant> load(int first, int pageSize, String sortField, int sortOrder, Map<String, String> filters, List<Applicant> list) {

        ArrayList<DBObject> parametros = new ArrayList<DBObject>();
        ArrayList<DBObject> parametrosGroup = new ArrayList<DBObject>();

        DBObject match = new BasicDBObject();

        DBObject matchProj = new BasicDBObject();
        matchProj.put("project.$id", currentProject.getId());
        matchProj.put("blacklisted", false);

        DBObject unwind = new BasicDBObject("$unwind", "$applicants");
        parametros.add(unwind);
        parametrosGroup.add(unwind);

        if (!filters.entrySet().isEmpty()) {
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
                matchFilterItem.put("applicants." + column, regex);
            }
            DBObject matchSearch = new BasicDBObject("$match", matchFilterItem);
            parametros.add(matchSearch);
            parametrosGroup.add(matchSearch);
        }

        DBObject matchFilter = null;
        if (list != null && !list.isEmpty()) {
            matchFilter = new BasicDBObject();
            BasicDBList lista = new BasicDBList();
            List<String> t = new ArrayList<String>();
            for (Applicant ap : list) {
                t.add(ap.getName());
            }
            lista.addAll(t);
            matchFilter.put("applicants.name", new BasicDBObject("$nin", lista));

            DBObject matchEdit = new BasicDBObject("$match", matchFilter);
            parametros.add(matchEdit);
            parametrosGroup.add(matchEdit);
        }

        DBObject idData = new BasicDBObject("name", "$applicants.name");
        idData.put("country", "$applicants.country");
        idData.put("state", "$applicants.state");
        idData.put("acronym", "$applicants.acronym");
        idData.put("nature", "$applicants.nature");
        idData.put("harmonized", "$applicants.harmonized");
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

        AggregationOutput outputTotal = null;

        outputTotal = ds.getCollection(Patent.class).aggregate(match, parametersGroup);

        BasicDBList outputListTotal = (BasicDBList) outputTotal.getCommandResult().get("result");
        for (Object patent : outputListTotal) {
            DBObject result = (DBObject) patent;
            this.setCount(Integer.parseInt(result.get("documentCount").toString()));
            break;
        }

        AggregationOutput output = ds.getCollection(Patent.class).aggregate(match, parameters);
        BasicDBList outputList = (BasicDBList) output.getCommandResult().get("result");

        List<Applicant> datasource = new ArrayList<Applicant>();
        for (Object patent : outputList) {
            DBObject aux = (DBObject) patent;
            DBObject result = (DBObject) aux.get("_id");
            Applicant applicant = new Applicant();
            applicant.setName(result.get("name").toString());
            if (result.get("acronym") != null) {
                applicant.setAcronym(result.get("acronym").toString());
            }
            DBObject nature = (DBObject) result.get("nature");
            if (nature != null) {
                ApplicantType realApplicantType = new ApplicantType();
                realApplicantType.setName((String) nature.get("name"));
                applicant.setType(realApplicantType);
            } else {
                applicant.setType(null);
            }

            DBObject country = (DBObject) result.get("country");
            if (country != null) {

                Country realCountry = new Country();
                realCountry.setAcronym((String) country.get("acronym"));
                realCountry.setName((String) country.get("name"));
                applicant.setCountry(realCountry);
            } else {
                applicant.setCountry(null);
            }

            DBObject state = (DBObject) result.get("state");
            if (state != null) {
                State realState = new State();
                realState.setAcronym((String) state.get("acronym"));
                realState.setRegion((String) state.get("region"));
                realState.setName((String) state.get("name"));
                applicant.setState(realState);
            } else {
                applicant.setState(null);
            }
            applicant.setHarmonized((Boolean) result.get("harmonized"));
            applicant.setDocumentCount((Integer) aux.get("documentCount"));
            datasource.add(applicant);
        }

        return datasource;
    }

     /**
     * Method responsible to search the data to fill the LazyTable of <b>Applicants</b>
     * @param first int - The offset of the query.
     * @param pageSize int - The limit of applicant in each page.
     * @param sortField String - Name of the column to be sorted.
     * @param sortOrder int - The Sort order of the column.
     * @param filters Map&lt;String, String&gt; - Map with the column and values of the filters in the table.
     * @return List&lt;Applicant&gt; - List of the applicants to be put in the table.
     */
    public List<Applicant> load(int first, int pageSize, String sortField, int sortOrder, Map<String, String> filters) {
        return load(first, pageSize, sortField, sortOrder, filters, null);
    }

    /**
     * Method responsible to verify the existence of an applicant in a project.
     * @param applicant - Applicant to be verified.
     * @return boolean - The existence or not of an applicant in a project.
     */
    public boolean exists(Applicant applicant) {

        ArrayList<DBObject> parametros = new ArrayList<DBObject>();

        DBObject matchProj = new BasicDBObject();
        matchProj.put("project.$id", currentProject.getId());
        matchProj.put("blacklisted", false);
        DBObject matchP = new BasicDBObject("$match", matchProj);

        DBObject unwind = new BasicDBObject("$unwind", "$applicants");
        parametros.add(unwind);

        DBObject fields = new BasicDBObject("applicants.country.acronym", applicant.getCountry().getAcronym());
        fields.put("applicants.name", applicant.getName());
        DBObject match = new BasicDBObject("$match", fields);
        parametros.add(match);

        DBObject idData = new BasicDBObject("name", "$applicants.name");
        DBObject field = new BasicDBObject("_id", idData);
        DBObject group = new BasicDBObject("$group", field);
        parametros.add(group);

        DBObject[] parameters = new DBObject[parametros.size()];
        parameters = parametros.toArray(parameters);

        AggregationOutput output = ds.getCollection(Patent.class).aggregate(matchP, parameters);

        BasicDBList outputList = (BasicDBList) output.getCommandResult().get("result");
        return outputList.size() == 0;

    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Project getCurrentProject() {
        return currentProject;
    }

    public void setCurrentProject(Project currentProject) {
        this.currentProject = currentProject;
    }

    public List<Applicant> getList() {
        return list;
    }

    public void setList(List<Applicant> list) {
        this.list = list;
    }

    public Integer getSearchType() {
        return searchType;
    }

    public void setSearchType(Integer searchType) {
        this.searchType = searchType;
    }
}
