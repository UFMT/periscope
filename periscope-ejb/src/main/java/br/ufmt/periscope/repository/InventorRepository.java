package br.ufmt.periscope.repository;

import br.ufmt.periscope.indexer.resources.search.FuzzyTokenSimilaritySearch;
import br.ufmt.periscope.model.Country;
import br.ufmt.periscope.model.Inventor;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.State;
import br.ufmt.periscope.report.Pair;
import br.ufmt.periscope.util.Filters;
import com.google.common.collect.HashMultiset;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Projections;
import dev.morphia.Datastore;
import dev.morphia.query.FindOptions;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bson.Document;
import org.bson.conversions.Bson;

/**
 * This class have the methods with the queries for Inventor
 */
@RequestScoped
@Named
public class InventorRepository {

    private @Inject
    Datastore ds;
    private Project currentProject;
    private int count;
    private Integer searchType;
    private @Inject
    FuzzyTokenSimilaritySearch fs;

    private MongoCollection<Document> patentDocs() {
        return ds.getCollection(Patent.class).withDocumentClass(Document.class);
    }

    public Inventor getInventorByName(String name) {
        long docCount = ds.find(Patent.class)
                .filter(dev.morphia.query.filters.Filters.elemMatch("inventors",
                        dev.morphia.query.filters.Filters.eq("name", name)))
                .count();
        Patent patent = ds.find(Patent.class)
                .filter(dev.morphia.query.filters.Filters.elemMatch("inventors",
                        dev.morphia.query.filters.Filters.eq("name", name)))
                .first();
        if (patent != null && patent.getInventors() != null) {
            for (Inventor inventor : patent.getInventors()) {
                if (inventor.getName() != null && inventor.getName().equals(name)) {
                    inventor.setDocumentCount((int) docCount);
                    return inventor;
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
        List<Bson> pipeline = new ArrayList<Bson>();

        pipeline.add(Aggregates.match(
                com.mongodb.client.model.Filters.eq("project.$id", currentProject.getId())));

        if (filtro.isComplete()) {
            pipeline.add(Aggregates.match(
                    com.mongodb.client.model.Filters.eq("completed", filtro.isComplete())));
        }

        if (filtro.getInicio() != null && filtro.getFim() != null) {
            if (filtro.getSelecionaData() == 1) {
                pipeline.add(Aggregates.match(new Document("publicationDate",
                        new Document("$gte", filtro.getInicio()).append("$lte", filtro.getFim()))));
            } else {
                pipeline.add(Aggregates.match(new Document("applicationDate",
                        new Document("$gte", filtro.getInicio()).append("$lte", filtro.getFim()))));
            }
        }

        pipeline.add(Aggregates.match(
                com.mongodb.client.model.Filters.eq("blacklisted", false)));
        pipeline.add(Aggregates.unwind("$inventors"));
        pipeline.add(Aggregates.group("$inventors",
                Accumulators.sum("applicationPerInventor", 1)));
        pipeline.add(Aggregates.sort(new Document("applicationPerInventor", -1)));
        pipeline.add(Aggregates.limit(limit));

        List<Document> outputResult = patentDocs().aggregate(pipeline)
                .into(new ArrayList<Document>());

        List<Pair> pairs = new ArrayList<Pair>();
        for (Document aux : outputResult) {
            Document inventorName = aux.get("_id", Document.class);
            if (inventorName == null || inventorName.get("name") == null) {
                continue;
            }
            String inventor = inventorName.get("name").toString();
            Integer countValue = toInt(aux.get("applicationPerInventor"));
            pairs.add(new Pair(inventor, countValue));
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

        FindOptions options = new FindOptions();
        options.projection().include("inventors");

        List<Patent> patents = ds.find(Patent.class)
                .filter(dev.morphia.query.filters.Filters.eq("project", currentProject),
                        dev.morphia.query.filters.Filters.exists("inventors"))
                .iterator(options)
                .toList();

        for (Patent patent : patents) {
            if (patent.getInventors() == null) {
                continue;
            }
            for (Inventor inv : patent.getInventors()) {
                bag.add(inv.getName());
                inv.setDocumentCount(bag.count(inv.getName()));
                map.put(inv.getName(), inv);
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
        List<Bson> pipeline = new ArrayList<Bson>();
        pipeline.add(Aggregates.match(com.mongodb.client.model.Filters.and(
                com.mongodb.client.model.Filters.eq("project.$id", project.getId()),
                com.mongodb.client.model.Filters.eq("blacklisted", false))));
        pipeline.add(Aggregates.unwind("$inventors"));
        pipeline.add(Aggregates.match(com.mongodb.client.model.Filters.regex(
                "inventors.name", "^" + java.util.regex.Pattern.quote(begins != null ? begins : ""), "i")));
        pipeline.add(Aggregates.project(Projections.include("inventors")));
        pipeline.add(Aggregates.group("$inventors.name"));

        List<Document> output = patentDocs().aggregate(pipeline).into(new ArrayList<Document>());
        List<String> lista = new ArrayList<String>();
        for (Document inventor : output) {
            Object id = inventor.get("_id");
            if (id != null) {
                lista.add(id.toString());
            }
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
            List<org.apache.lucene.document.Document> docs =
                    fs.search("inventor", project.getId().toString(), name, top);
            for (org.apache.lucene.document.Document doc : docs) {
                results.add(doc.get("inventor"));
            }
        }

        return results;
    }

    public List<Inventor> load(int first, int pageSize, String sortField, int sortOrder,
            Map<String, String> filters, List<Inventor> list) {

        Document matchProj = new Document("project.$id", currentProject.getId())
                .append("blacklisted", false);

        List<Bson> parametros = new ArrayList<Bson>();
        List<Bson> parametrosGroup = new ArrayList<Bson>();

        parametros.add(Aggregates.unwind("$inventors"));
        parametrosGroup.add(Aggregates.unwind("$inventors"));

        Document matchFilterItem = new Document();
        if (filters != null) {
            for (Map.Entry<String, String> entry : filters.entrySet()) {
                String column = entry.getKey();
                String value = java.util.regex.Pattern.quote(entry.getValue());
                Document regexDoc;
                if (searchType != null && searchType.equals(1)) {
                    regexDoc = new Document("$regex", "^" + value).append("$options", "i");
                } else {
                    regexDoc = new Document("$regex", value).append("$options", "i");
                }
                matchFilterItem.put("inventors." + column, regexDoc);
            }
        }

        if (list != null) {
            List<String> names = new ArrayList<String>();
            for (Inventor inv : list) {
                names.add(inv.getName());
            }
            matchFilterItem.put("inventors.name", new Document("$nin", names));
        }

        if (!matchFilterItem.keySet().isEmpty()) {
            Bson matchEdit = Aggregates.match(matchFilterItem);
            parametros.add(matchEdit);
            parametrosGroup.add(matchEdit);
        }

        Document idData = new Document("name", "$inventors.name")
                .append("country", "$inventors.country")
                .append("state", "$inventors.state")
                .append("acronym", "$inventors.acronym")
                .append("harmonized", "$inventors.harmonized");
        Document groupFields = new Document("_id", idData)
                .append("documentCount", new Document("$sum", 1));
        Bson group = new Document("$group", groupFields);
        parametros.add(group);
        parametrosGroup.add(group);

        Document groupTotalFields = new Document("_id", "nome")
                .append("documentCount", new Document("$sum", 1));
        parametrosGroup.add(new Document("$group", groupTotalFields));

        if (sortField != null) {
            if ("documentCount".equals(sortField)) {
                parametros.add(Aggregates.sort(new Document(sortField, sortOrder == 0 ? 1 : -1)));
            } else {
                parametros.add(Aggregates.sort(
                        new Document("_id." + sortField, sortOrder == 0 ? 1 : -1)));
            }
        } else {
            parametros.add(Aggregates.sort(new Document("_id.name", 1)));
        }

        parametros.add(Aggregates.skip(first));
        parametros.add(Aggregates.limit(pageSize));

        List<Bson> totalPipeline = new ArrayList<Bson>();
        totalPipeline.add(Aggregates.match(matchProj));
        totalPipeline.addAll(parametrosGroup);

        this.setCount(0);
        List<Document> outputListTotal = patentDocs().aggregate(totalPipeline)
                .into(new ArrayList<Document>());
        for (Document result : outputListTotal) {
            this.setCount(toInt(result.get("documentCount")));
            break;
        }

        List<Bson> pagePipeline = new ArrayList<Bson>();
        pagePipeline.add(Aggregates.match(matchProj));
        pagePipeline.addAll(parametros);

        List<Document> outputList = patentDocs().aggregate(pagePipeline)
                .into(new ArrayList<Document>());

        List<Inventor> datasource = new ArrayList<Inventor>();
        for (Document aux : outputList) {
            Document result = aux.get("_id", Document.class);
            if (result == null || result.get("name") == null) {
                continue;
            }
            Inventor inventor = new Inventor();
            inventor.setName(result.get("name").toString());
            if (result.get("acronym") != null) {
                inventor.setAcronym(result.get("acronym").toString());
            }

            Document country = result.get("country", Document.class);
            if (country != null) {
                Country realCountry = new Country();
                realCountry.setAcronym((String) country.get("acronym"));
                realCountry.setName((String) country.get("name"));
                inventor.setCountry(realCountry);
            } else {
                inventor.setCountry(null);
            }

            Document state = result.get("state", Document.class);
            if (state != null) {
                State realState = new State();
                realState.setAcronym((String) state.get("acronym"));
                realState.setRegion((String) state.get("region"));
                realState.setName((String) state.get("name"));
                inventor.setState(realState);
            } else {
                inventor.setState(null);
            }
            inventor.setHarmonized(result.getBoolean("harmonized"));
            inventor.setDocumentCount(toInt(aux.get("documentCount")));
            datasource.add(inventor);
        }

        return datasource;
    }

    public boolean exists(Inventor inventor) {
        List<Bson> pipeline = new ArrayList<Bson>();
        pipeline.add(Aggregates.match(new Document("project.$id", currentProject.getId())
                .append("blacklisted", false)));
        pipeline.add(Aggregates.unwind("$inventors"));
        pipeline.add(Aggregates.match(new Document("inventors.country.acronym",
                inventor.getCountry().getAcronym())
                .append("inventors.name", inventor.getName())));
        pipeline.add(Aggregates.group(new Document("name", "$inventors.name")));

        List<Document> outputList = patentDocs().aggregate(pipeline)
                .into(new ArrayList<Document>());
        return outputList.isEmpty();
    }

    public List<Inventor> load(int first, int pageSize, String sortField, int sortOrder,
            Map<String, String> filters) {
        return load(first, pageSize, sortField, sortOrder, filters, null);
    }

    private static int toInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(value.toString());
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
