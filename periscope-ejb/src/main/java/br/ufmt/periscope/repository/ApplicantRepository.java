package br.ufmt.periscope.repository;

import br.ufmt.periscope.indexer.resources.search.FuzzyTokenSimilaritySearch;
import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.model.ApplicantType;
import br.ufmt.periscope.model.Country;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.State;
import br.ufmt.periscope.util.Filters;
import com.google.common.collect.HashMultiset;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Projections;
import dev.morphia.Datastore;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Sort;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bson.Document;
import org.bson.conversions.Bson;

/**
 * This class have the methods with the queries for Applicant
 */
@RequestScoped
@Named
public class ApplicantRepository {

    private @Inject
    Datastore ds;
    private Project currentProject;
    private int count;
    private List<Applicant> list;
    private Integer searchType;
    private @Inject
    FuzzyTokenSimilaritySearch fs;

    private MongoCollection<Document> patentDocs() {
        return ds.getCollection(Patent.class).withDocumentClass(Document.class);
    }

    /**
     * Method that query an applicant by its name.
     *
     * @param name String - Name of the applicant to be got.
     * @return Applicant
     */
    public Applicant getApplicantByName(String name) {
        long docCount = ds.find(Patent.class)
                .filter(dev.morphia.query.filters.Filters.elemMatch("applicants",
                        dev.morphia.query.filters.Filters.eq("name", name)))
                .count();
        Patent patent = ds.find(Patent.class)
                .filter(dev.morphia.query.filters.Filters.elemMatch("applicants",
                        dev.morphia.query.filters.Filters.eq("name", name)))
                .first();
        if (patent != null && patent.getApplicants() != null) {
            for (Applicant applicant : patent.getApplicants()) {
                if (applicant.getName() != null && applicant.getName().equals(name)) {
                    applicant.setDocumentCount((int) docCount);
                    return applicant;
                }
            }
        }
        return null;
    }

    /**
     * Method responsible to get the applicants from database.
     *
     * @param project Project - Project in which the applicant most be searched.
     * @return List&lt;String&gt; - List with the applicants queried.
     */
    public List<Applicant> getApplicants(Project project) {
        Map<String, Applicant> map = new HashMap<String, Applicant>();
        HashMultiset<String> bag = HashMultiset.create();

        FindOptions options = new FindOptions();
        options.projection().include("applicants");
        options.sort(Sort.ascending("applicants.name"));

        List<Patent> patents = ds.find(Patent.class)
                .filter(dev.morphia.query.filters.Filters.eq("project", project),
                        dev.morphia.query.filters.Filters.exists("applicants"))
                .iterator(options)
                .toList();

        for (Patent patent : patents) {
            if (patent.getApplicants() == null) {
                continue;
            }
            for (Applicant pa : patent.getApplicants()) {
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
     *
     * @param project Project - Project in which the applicant most be searched.
     * @param begins String - The applicant name must begins with this string
     * @return List&lt;String&gt; - List with the applicants queried.
     */
    public List<String> getApplicants(Project project, String begins) {
        List<Bson> pipeline = new ArrayList<Bson>();
        pipeline.add(Aggregates.match(com.mongodb.client.model.Filters.and(
                com.mongodb.client.model.Filters.eq("project.$id", project.getId()),
                com.mongodb.client.model.Filters.eq("blacklisted", false))));
        pipeline.add(Aggregates.unwind("$applicants"));
        pipeline.add(Aggregates.match(com.mongodb.client.model.Filters.regex(
                "applicants.name", "^" + java.util.regex.Pattern.quote(begins != null ? begins : ""), "i")));
        pipeline.add(Aggregates.project(Projections.include("applicants")));
        pipeline.add(Aggregates.group("$applicants.name"));

        List<Document> output = patentDocs().aggregate(pipeline).into(new ArrayList<Document>());
        List<String> lista = new ArrayList<String>();
        for (Document applicant : output) {
            Object id = applicant.get("_id");
            if (id != null) {
                lista.add(id.toString());
            }
        }
        return lista;
    }

    /**
     * Method responsible to update the mainApplicant collection with an
     * aggregation pipeline (replaces legacy MapReduce).
     *
     * Produces documents {@code {_id: applicantName, value: count}} consumed by
     * {@code MainApplicantReport}.
     *
     * @param currentProject Project - Current Project.
     * @param filtro Filters - filters to be applied in the query.
     */
    public void updateMainApplicants(Project currentProject, Filters filtro) {
        Document match = new Document("project.$id", currentProject.getId())
                .append("blacklisted", false)
                .append("applicants", new Document("$exists", true));
        if (filtro != null) {
            if (filtro.isComplete()) {
                match.append("completed", true);
            }
            if (filtro.getInicio() != null && filtro.getFim() != null) {
                String dateField = filtro.getSelecionaData() == 1 ? "publicationDate" : "applicationDate";
                match.append(dateField,
                        new Document("$gte", filtro.getInicio()).append("$lte", filtro.getFim()));
            }
            if (filtro.getApplicantType() != null && !filtro.getApplicantType().isEmpty()) {
                match.append("$or", Arrays.asList(
                        new Document("applicants.type.name", filtro.getApplicantType()),
                        new Document("applicants.nature.name", filtro.getApplicantType())));
            }
        }

        List<Bson> pipeline = Arrays.<Bson>asList(
                Aggregates.match(match),
                Aggregates.unwind("$applicants"),
                Aggregates.group("$applicants.name", Accumulators.sum("value", 1.0)),
                Aggregates.out("mainApplicant"));

        patentDocs().aggregate(pipeline).toCollection();
    }

    /**
     * Method responsible for searching with <i>LUCENE</i> the suggestions of
     * Applicants for the harmonization.
     *
     * @param project Project - Project where the suggestions should be
     * searched.
     * @param top int - Maximum amount of names to be returned.
     * @param names String - Names of applicants to be queried.
     * @return Set&lt;String&gt; - Set of applicants names suggested by the
     * query
     */
    public Set<String> getApplicantSugestions(Project project, int top, String... names) {
        Set<String> results = new HashSet<String>();

        for (String name : names) {
            List<org.apache.lucene.document.Document> docs =
                    fs.search("applicant", project.getId().toString(), name, top);
            for (org.apache.lucene.document.Document doc : docs) {
                results.add(doc.get("applicant"));
            }
        }

        return results;
    }

    /**
     * Method responsible to search the data to fill the LazyTable of
     * <b>Applicants</b>
     *
     * @param first int - The offset of the query.
     * @param pageSize int - The limit of applicant in each page.
     * @param sortField String - Name of the column to be sorted.
     * @param sortOrder int - The Sort order of the column.
     * @param filters Map&lt;String, String&gt; - Map with the column and values
     * of the filters in the table.
     * @param list List&lt;Applicant&gt; - List with the applicants that should
     * not be queried.
     * @return List&lt;Applicant&gt; - List of the applicants to be put in the
     * table.
     */
    public List<Applicant> load(int first, int pageSize, String sortField, int sortOrder,
            Map<String, String> filters, List<Applicant> list) {

        Document matchProj = new Document("project.$id", currentProject.getId())
                .append("blacklisted", false);

        List<Bson> parametros = new ArrayList<Bson>();
        List<Bson> parametrosGroup = new ArrayList<Bson>();

        parametros.add(Aggregates.unwind("$applicants"));
        parametrosGroup.add(Aggregates.unwind("$applicants"));

        if (filters != null && !filters.entrySet().isEmpty()) {
            Document matchFilterItem = new Document();
            for (Map.Entry<String, String> entry : filters.entrySet()) {
                String column = entry.getKey();
                String value = java.util.regex.Pattern.quote(entry.getValue());
                Document regexDoc;
                if (searchType != null && searchType.equals(1)) {
                    regexDoc = new Document("$regex", "^" + value).append("$options", "i");
                } else {
                    regexDoc = new Document("$regex", value).append("$options", "i");
                }
                matchFilterItem.put("applicants." + column, regexDoc);
            }
            Bson matchSearch = Aggregates.match(matchFilterItem);
            parametros.add(matchSearch);
            parametrosGroup.add(matchSearch);
        }

        if (list != null && !list.isEmpty()) {
            List<String> names = new ArrayList<String>();
            for (Applicant ap : list) {
                names.add(ap.getName());
            }
            Document matchFilter = new Document("applicants.name",
                    new Document("$nin", names));
            Bson matchEdit = Aggregates.match(matchFilter);
            parametros.add(matchEdit);
            parametrosGroup.add(matchEdit);
        }

        Document idData = new Document("name", "$applicants.name")
                .append("country", "$applicants.country")
                .append("state", "$applicants.state")
                .append("acronym", "$applicants.acronym")
                .append("type", "$applicants.type")
                .append("nature", "$applicants.nature")
                .append("harmonized", "$applicants.harmonized");
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

        List<Applicant> datasource = new ArrayList<Applicant>();
        for (Document aux : outputList) {
            Document result = aux.get("_id", Document.class);
            if (result == null) {
                continue;
            }
            Applicant applicant = new Applicant();
            if (result.get("name") != null) {
                applicant.setName(result.get("name").toString());
            }
            if (result.get("acronym") != null) {
                applicant.setAcronym(result.get("acronym").toString());
            }
            Document nature = result.get("type", Document.class);
            if (nature == null) {
                nature = result.get("nature", Document.class);
            }
            if (nature != null) {
                ApplicantType realApplicantType = new ApplicantType();
                realApplicantType.setName((String) nature.get("name"));
                applicant.setType(realApplicantType);
            } else {
                applicant.setType(null);
            }

            Document country = result.get("country", Document.class);
            if (country != null) {
                Country realCountry = new Country();
                realCountry.setAcronym((String) country.get("acronym"));
                realCountry.setName((String) country.get("name"));
                applicant.setCountry(realCountry);
            } else {
                applicant.setCountry(null);
            }

            Document state = result.get("state", Document.class);
            if (state != null) {
                State realState = new State();
                realState.setAcronym((String) state.get("acronym"));
                realState.setRegion((String) state.get("region"));
                realState.setName((String) state.get("name"));
                applicant.setState(realState);
            } else {
                applicant.setState(null);
            }
            Boolean harmonized = result.getBoolean("harmonized");
            applicant.setHarmonized(harmonized);
            applicant.setDocumentCount(toInt(aux.get("documentCount")));
            datasource.add(applicant);
        }

        return datasource;
    }

    /**
     * Method responsible to search the data to fill the LazyTable of
     * <b>Applicants</b>
     *
     * @param first int - The offset of the query.
     * @param pageSize int - The limit of applicant in each page.
     * @param sortField String - Name of the column to be sorted.
     * @param sortOrder int - The Sort order of the column.
     * @param filters Map&lt;String, String&gt; - Map with the column and values
     * of the filters in the table.
     * @return List&lt;Applicant&gt; - List of the applicants to be put in the
     * table.
     */
    public List<Applicant> load(int first, int pageSize, String sortField, int sortOrder,
            Map<String, String> filters) {
        return load(first, pageSize, sortField, sortOrder, filters, null);
    }

    /**
     * Method responsible to verify the existence of an applicant in a project.
     *
     * @param applicant - Applicant to be verified.
     * @return boolean - The existence or not of an applicant in a project.
     */
    public boolean exists(Applicant applicant) {
        List<Bson> pipeline = new ArrayList<Bson>();
        pipeline.add(Aggregates.match(new Document("project.$id", currentProject.getId())
                .append("blacklisted", false)));
        pipeline.add(Aggregates.unwind("$applicants"));
        pipeline.add(Aggregates.match(new Document("applicants.country.acronym",
                applicant.getCountry().getAcronym())
                .append("applicants.name", applicant.getName())));
        pipeline.add(Aggregates.group(new Document("name", "$applicants.name")));

        List<Document> outputList = patentDocs().aggregate(pipeline)
                .into(new ArrayList<Document>());
        return outputList.isEmpty();
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
