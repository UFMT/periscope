package br.ufmt.periscope.harmonization;

import br.ufmt.periscope.indexer.PatentIndexer;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Rule;
import com.github.jmkgreen.morphia.Datastore;
import com.github.jmkgreen.morphia.mapping.Mapper;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import java.util.ArrayList;
import javax.inject.Inject;
import javax.inject.Named;
import org.bson.types.ObjectId;

@Named
public class Harmonization {

    private @Inject
    Datastore ds;
    private @Inject
    PatentIndexer indexer;

    /**
     * Apply the rule
     * @param rule
     */
    public void applyRule(Rule rule) {
        if (rule == null) {
            return;
        }
        switch (rule.getType()) {
            case APPLICANT:
                applyApplicantRule(rule);
                break;
            case INVENTOR:
                applyInventorRule(rule);
                break;
            default:
                break;

        }
    }

    private void applyApplicantRule(Rule rule) {
        ObjectId projectId = rule.getProject().getId();
        Mapper mapper = ds.getMapper();
        rule.getCountry().setStates(null);
        DBObject dbObjectCountry = mapper.toDBObject(rule.getCountry());
        DBObject dbObjectState = mapper.toDBObject(rule.getState());
        DBObject dbObjectNature = mapper.toDBObject(rule.getNature());
        String applicants[] = new String[rule.getSubstitutions().size()];
        applicants = rule.getSubstitutions().toArray(applicants);
        for (String applicant : applicants) {
            DBObject query = BasicDBObjectBuilder
                    .start("project.$id", projectId).add("applicants.name", applicant).get();
            DBObject updateOp = BasicDBObjectBuilder
                    .start("$set",
                            BasicDBObjectBuilder
                            .start("applicants.$.name", rule.getName())
                            .add("applicants.$.country", dbObjectCountry)
                            .add("applicants.$.state", dbObjectState)
                            .add("applicants.$.nature", dbObjectNature)
                            .add("applicants.$.acronym", rule.getAcronym())
                            .get())
                    .get();
            ds.getCollection(Patent.class)
                    .updateMulti(query, updateOp);
        }
//        ini = System.currentTimeMillis();
        indexer.indexRule(new ArrayList<String>(rule.getSubstitutions()), rule.getName(), null, null, rule.getProject());
//        System.out.println("Tempo indexar "+(System.currentTimeMillis() - ini));

    }

    private void applyInventorRule(Rule rule) {
        ObjectId projectId = rule.getProject().getId();
        Mapper mapper = ds.getMapper();
        rule.getCountry().setStates(null);
        DBObject dbObjectCountry = mapper.toDBObject(rule.getCountry());
        DBObject dbObjectState = mapper.toDBObject(rule.getState());
        DBObject dbObjectNature = mapper.toDBObject(rule.getNature());
        String inventors[] = new String[rule.getSubstitutions().size()];
        inventors = rule.getSubstitutions().toArray(inventors);
        for (String inventor : inventors) {
            DBObject query = BasicDBObjectBuilder
                    .start("project.$id", projectId)
                    .add("inventors.name", inventor)
                    .get();
            DBObject updateOp = BasicDBObjectBuilder
                    .start("$set",
                            BasicDBObjectBuilder
                            .start("inventors.$.name", rule.getName())
                            .add("inventors.$.country", dbObjectCountry)
                            .add("inventors.$.state", dbObjectState)
                            .add("inventors.$.nature", dbObjectNature)
                            .add("inventors.$.acronym", rule.getAcronym())
                            .get())
                    .get();

            ds.getCollection(Patent.class)
                    .updateMulti(query, updateOp);
        }

        indexer.indexRule(null, null, new ArrayList<String>(rule.getSubstitutions()), rule.getName(), rule.getProject());

    }
}
