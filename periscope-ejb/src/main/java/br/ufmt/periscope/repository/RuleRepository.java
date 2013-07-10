package br.ufmt.periscope.repository;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.bson.types.ObjectId;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.Rule;
import br.ufmt.periscope.model.RuleType;

import com.github.jmkgreen.morphia.Datastore;

@Named
public class RuleRepository {

	@Inject
	private Datastore ds;
					
	public List<Rule> getAllRule(Project project){
		return ds.find(Rule.class)
				.field("project").equal(project)
				.asList();
	}
	
	public List<Rule> getApplicantRule(Project project){
		return ds.find(Rule.class)
				.field("project").equal(project)
				.field("type").equal(RuleType.APPLICANT)
				.asList();
	}
	
	public List<Rule> getInventorRule(Project project){
		return ds.find(Rule.class)
				.field("project").equal(project)
				.field("type").equal(RuleType.INVENTOR)
				.asList();
	}
	
	public Rule findById(String id){
		return ds.get(Rule.class, new ObjectId(id));
	}
	
	public void delete(String id){
		ds.delete(Rule.class, new ObjectId(id));
	}
}
