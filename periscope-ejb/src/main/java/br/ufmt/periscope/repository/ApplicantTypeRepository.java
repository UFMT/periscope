package br.ufmt.periscope.repository;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import br.ufmt.periscope.model.ApplicantType;

import com.github.jmkgreen.morphia.Datastore;
import com.mongodb.BasicDBObject;

@Named
public class ApplicantTypeRepository {

	private @Inject Datastore ds;
	
	public void createIfNotExists(ApplicantType type){
		BasicDBObject where = new BasicDBObject("name",type.getName());
		int count = ds.getCollection(ApplicantType.class).find(where).count();
		
		if(count >= 0) return;
		
		ds.save(type);
	}	
	
	public List<ApplicantType> getAll(){
		return ds.createQuery(ApplicantType.class).order("name").asList();
	}
}
