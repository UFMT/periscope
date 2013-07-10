package br.ufmt.periscope.harmonization;

import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.bson.types.ObjectId;

import com.github.jmkgreen.morphia.Datastore;
import com.github.jmkgreen.morphia.mapping.Mapper;
import com.github.jmkgreen.morphia.mapping.cache.EntityCache;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;

import br.ufmt.periscope.indexer.PatentIndexer;
import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Rule;
import br.ufmt.periscope.repository.ApplicantRepository;
import br.ufmt.periscope.repository.PatentRepository;

@Named
public class Harmonization {
	
	private @Inject Datastore ds;
	private @Inject PatentRepository patentRepository; 
	private @Inject PatentIndexer indexer;

	public void applyRule(Rule rule){
		if(rule == null) return;
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
	
	private void applyApplicantRule(Rule rule){
		ObjectId projectId = rule.getProject().getId();
		System.out.println(projectId);
		Mapper mapper = ds.getMapper();	
		DBObject dbObjectCountry = mapper.toDBObject(rule.getCountry());
		DBObject dbObjectNature = mapper.toDBObject(rule.getNature());
		System.out.println(dbObjectCountry);
		System.out.println(dbObjectNature);
		DBObject query = BasicDBObjectBuilder
									.start("project.$id",projectId)
									.add("applicants.name",BasicDBObjectBuilder
															.start("$in", rule.getSubstitutions())
															.get())
									.get();
		DBObject updateOp = BasicDBObjectBuilder
									.start("$set",
										BasicDBObjectBuilder
										.start("applicants.$.name",rule.getName())
										.add("applicants.$.country",dbObjectCountry)
										.add("applicants.$.nature",dbObjectNature)
										.get())
									.get();
		System.out.println(query);
		System.out.println(updateOp);
		ds.getCollection(Patent.class).updateMulti(query,updateOp);
				
		List<Patent> patents = patentRepository.getPatentWithApplicant(rule.getProject(), rule.getName());
		for (Patent patent : patents) {
			System.out.println(patent.getProject().getId().toString() + patent.getPublicationNumber());
		}
		indexer.indexPatents(patents);
		
	}

	private void applyInventorRule(Rule rule){
		
	}
}
