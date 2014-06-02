package br.ufmt.periscope.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.util.Version;

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
import java.io.StringReader;
import java.util.Collections;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.FuzzyQuery;

@Named
public class ApplicantRepository {

    private @Inject
    Datastore ds;
    private @Inject
    IndexReader reader;
    private @Inject
    Analyzer analyzer;
    private @Inject
    Project currentProject;
    private int count;
    private List<Applicant> list;

    public Applicant getApplicantByName(String name) {

        Applicant ret = null;
        BasicDBObject eleMatch = new BasicDBObject("$elemMatch", new BasicDBObject("name", name));
        BasicDBObject applicants = new BasicDBObject("applicants", eleMatch);
        BasicDBObject keys = new BasicDBObject("applicants", 1);
//            applicants.put("applicants", 1);
        DBCursor cursor = ds.getCollection(Patent.class).find(applicants, keys).limit(1);
        if (cursor.hasNext()) {
            Mapper mapper = ds.getMapper();
            EntityCache ec = mapper.createEntityCache();

            BasicDBList objList = (BasicDBList) cursor.next().get("applicants");
            Iterator<Object> itList = objList.iterator();
            if (itList.hasNext()) {
                ret = (Applicant) mapper.fromDBObject(Applicant.class, (DBObject) itList.next(), ec);
            }
            System.out.println("fim");
            return ret;
        }
        System.out.println("NULO");
        return null;

    }

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

        DBCollection coll = ds.getCollection(Patent.class);
        MapReduceCommand cmd = new MapReduceCommand(coll,
                map,
                reduce,
                "mainApplicant",
                OutputType.REPLACE,
                where);
        coll.mapReduce(cmd);

    }

    public Set<String> getApplicantSugestions(Project project, int top, String... names) {

        Set<String> results = new HashSet<String>();
        try {
//            StringBuilder queryBuilder = new StringBuilder();
//            for (String name : names) {
//                String[] terms = name.split(" ", -2);
//                for (String term : terms) {
//                    //if(term.length() >= 4){		
//                    queryBuilder.append(term + "~ ");
//                    queryBuilder.append(term + "* ");
//                    //}
//                }
//
//                //queryBuilder.append("NOT \""+name+"\" ");	
//                queryBuilder.append("\"" + name + "\"~10 ");
//            }

            Query queryProject = new QueryParser(Version.LUCENE_36, "project", analyzer)
                    .parse(project.getId().toString());
            queryProject.setBoost(0.1f);

            IndexSearcher searcher = new IndexSearcher(reader);

            for (String name : names) {
                // Cria uma stream de tokens com o analyzer
                TokenStream stream = analyzer.tokenStream("applicant", new StringReader(name));
                // Passa os atributos da stream para que seja possível recuperar seu valor puro de texto
                CharTermAttribute attr = stream.getAttribute(CharTermAttribute.class);
                // resetar a stream é necessário fazer, não sei o porque, mais se não o fizer da erro
                stream.reset();
                // Recuperando o valor de texto da stream
                name = "";
                while (stream.incrementToken()) {
                    name = name + attr.toString();
                }
                stream.end();
                stream.close();

                TopScoreDocCollector collector = TopScoreDocCollector.create(1000, true);
                BooleanQuery bq = new BooleanQuery();
                //Query queryPa = new QueryParser(Version.LUCENE_36, "applicant", analyzer)
                //       .parse(queryBuilder.toString());
                //queryPa.setBoost(10f);

                Query query = new FuzzyQuery(new Term("applicant", name), 0.8f);

                bq.add(query, Occur.MUST);
                bq.add(queryProject, Occur.MUST);
                System.out.println(bq);

                searcher.search(bq, collector);

                ScoreDoc[] hits = collector.topDocs().scoreDocs;
                System.out.println("Found " + hits.length + " hits.");
                for (int i = 0; i < hits.length; ++i) {
                    int docId = hits[i].doc;
                    Document d = searcher.doc(docId);
                    System.out.println((i + 1) + ". " + d.get("applicant") + "\t" + hits[i].score);
                    results.add(d.get("applicant"));

                    if (results.size() == top) {
                        break;
                    }
                }
            }
            for (String name : names) {
                results.remove(name);
            }
            searcher.close();

            return results;

        } catch (CorruptIndexException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return results;

    }

    public List<Applicant> load(int first, int pageSize, String sortField, int sortOrder, Map<String, String> filters, List<Applicant> list) {

        ArrayList<DBObject> parametros = new ArrayList<DBObject>();

        DBObject match = new BasicDBObject();

        DBObject matchProj = new BasicDBObject();
        matchProj.put("project.$id", currentProject.getId());
        matchProj.put("blacklisted", false);

        DBObject unwind = new BasicDBObject("$unwind", "$applicants");
        parametros.add(unwind);

        DBObject matchFilterItem = new BasicDBObject();
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            String column = entry.getKey();
            String value = entry.getValue();
            DBObject regex = new BasicDBObject("$regex", value).append("$options", "i");
            matchFilterItem.put("applicants." + column, regex);
        }
        DBObject matchSearch = new BasicDBObject("$match", matchFilterItem);
        parametros.add(matchSearch);
        
        
        DBObject matchFilter = new BasicDBObject();
        if (list != null){
            BasicDBList lista = new BasicDBList();
            List<String> t = new ArrayList<String>();
            for (Applicant ap : list) {
                t.add(ap.getName());
            }
            lista.addAll(t);
            matchFilter.put("applicants.name", new BasicDBObject("$nin", lista));
        }
        DBObject matchEdit = new BasicDBObject("$match", matchFilter);
        parametros.add(matchEdit);
        
        

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

        fields = new BasicDBObject("_id", "nome");
        fields.put("documentCount", new BasicDBObject("$sum", 1));
        DBObject groupTotal = new BasicDBObject();
        groupTotal.put("$group", fields);

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

        match.put("$match", matchProj);

        AggregationOutput outputTotal = ds.getCollection(Patent.class).aggregate(match, unwind, matchEdit, matchSearch, group, groupTotal);
        System.out.println("CHEGOU AQUI");
        System.out.println(outputTotal.getCommand().toString());

        BasicDBList outputListTotal = (BasicDBList) outputTotal.getCommandResult().get("result");
        for (Object patent : outputListTotal) {
            DBObject result = (DBObject) patent;
            this.setCount(Integer.parseInt(result.get("documentCount").toString()));
            break;
        }

        AggregationOutput output = ds.getCollection(Patent.class).aggregate(match, parameters);
        System.out.println(output.getCommand().toString());
        BasicDBList outputList = (BasicDBList) output.getCommandResult().get("result");

//        this.setCount(output.);
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
    
    public List<Applicant> load(int first, int pageSize, String sortField, int sortOrder, Map<String, String> filters){
        return load(first, pageSize, sortField, sortOrder, filters, null);
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
    
    
}
