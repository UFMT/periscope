package br.ufmt.periscope.repository;

import br.ufmt.periscope.indexer.resources.search.LengthQuery;
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
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.util.Version;

@Named
public class InventorRepository {

    private @Inject
    Datastore ds;
    private @Inject
    IndexReader reader;
    private @Inject
    Analyzer analyzer;
    private @Inject
    Project currentProject;
    private int count;
    private Integer searchType;

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

//        System.out.println(limit);
        DBObject pipeLimit = new BasicDBObject("$limit", limit);
        parametros.add(pipeLimit);

        DBObject[] parameters = new DBObject[parametros.size()];
        parameters = parametros.toArray(parameters);

        AggregationOutput output = ds.getCollection(Patent.class).aggregate(matchProj, parameters);
//        System.out.println("query " + output.getCommand());

        BasicDBList outputResult = (BasicDBList) output.getCommandResult().get(
                "result");

        List<Pair> pairs = new ArrayList<Pair>();
        for (Object object : outputResult) {
            DBObject aux = (DBObject) object;
            DBObject inventorName = (DBObject) aux.get("_id");
            String inventor = inventorName.get("name").toString();
            Integer count = (Integer) aux.get("applicationPerInventor");

            pairs.add(new Pair(inventor, count));
//            System.out.println(inventor + " " + count);
        }
        return pairs;
    }

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
//        System.out.println(output.getCommand());
        BasicDBList outputList = (BasicDBList) output.getCommandResult().get("result");
        List<String> lista = new ArrayList<String>();
        for (Object inventor : outputList) {
            DBObject aux = (DBObject) inventor;
            String nome = aux.get("_id").toString();
            lista.add(nome);
        }
        return lista;

    }

    public Set<String> getInventorSugestions(Project project, int top, String... names) {

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

            Query queryProject = new QueryParser(Version.LUCENE_47, "project", analyzer)
                    .parse(project.getId().toString());
            queryProject.setBoost(0.1f);

            IndexSearcher searcher = new IndexSearcher(reader);

            for (String name : names) {
                // Cria uma stream de tokens com o analyzer
                TokenStream stream = analyzer.tokenStream("inventor", new StringReader(name));
                // Passa os atributos da stream para que seja possível recuperar seu valor puro de texto
                CharTermAttribute attr = stream.getAttribute(CharTermAttribute.class);
                stream.reset();
                // Recuperando o valor de texto da stream
                name = "";
                while (stream.incrementToken()) {
                    name = name + attr.toString() + " ";
                }
                name = name.trim();
                stream.end();
                stream.close();

                TopScoreDocCollector collector = TopScoreDocCollector.create(1000, true);
                BooleanQuery bq = new BooleanQuery();
//                System.out.println(name);
                // Criando a query, dar o name.split é para saber a existência do acrônimo
                String[] tokens = name.split(" ");
                // Se for maior que 1, existe um acrônimo
                if (tokens.length > 1) {
//                    System.out.println(tokens[0]);
                    bq.add(new PrefixQuery(new Term("inventor", tokens[0])), BooleanClause.Occur.MUST);
//                    System.out.println(tokens[1]);
                    bq.add(new FuzzyQuery(new Term("inventor", tokens[1]), 1), BooleanClause.Occur.MUST);
                    bq.add(new LengthQuery("inventor", name), BooleanClause.Occur.MUST_NOT);
                } else {
                    if (name.length() > 3) {
                        bq.add(new FuzzyQuery(new Term("inventor", name)),
                                BooleanClause.Occur.MUST);
                    } else {
                        bq.add(new PrefixQuery(new Term("inventor", name)),
                                BooleanClause.Occur.MUST);
                    }
                }


                bq.add(queryProject, BooleanClause.Occur.MUST);
//                System.out.println(bq);

                ScoreDoc[] hits = searcher.search(bq, 1000).scoreDocs;

                //ScoreDoc[] hits = collector.topDocs().scoreDocs;
                //System.out.println("Found " + hits.length + " hits.");
                for (int i = 0; i < hits.length; ++i) {
                    int docId = hits[i].doc;
                    Document d = searcher.doc(docId);
//                  System.out.println((i + 1) + ". " + d.get("applicant") + "\t" + hits[i].score);
                    results.add(d.get("inventor"));

                    if (results.size() == top) {
                        break;
                    }
                }
            }
            for (String name : names) {
                results.remove(name);
            }
            //searcher.close();

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
//            System.out.println(matchFilterItem.toString());
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
        long out1 = System.currentTimeMillis();
        AggregationOutput outputTotal = ds.getCollection(Patent.class).aggregate(match, parametersGroup);
//        System.out.println("1ª Consulta Inventores: " + outputTotal.getCommand());
//        System.out.println("Tempo 1º output: " + (System.currentTimeMillis() - out1));
        BasicDBList outputListTotal = (BasicDBList) outputTotal.getCommandResult().get("result");
        for (Object patent : outputListTotal) {
            DBObject result = (DBObject) patent;
            this.setCount(Integer.parseInt(result.get("documentCount").toString()));
            break;
        }
        long out2 = System.currentTimeMillis();
        AggregationOutput output = ds.getCollection(Patent.class).aggregate(match, parameters);
//        System.out.println("2ª Consulta Inventores: " + output.getCommand().toString());
//        System.out.println("Tempo 2º output: " + (System.currentTimeMillis() - out2));

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

//        System.out.println("entrou aqui");
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

//        System.out.println("foi antes");
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
