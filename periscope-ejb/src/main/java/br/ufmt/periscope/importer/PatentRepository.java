package br.ufmt.periscope.importer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;

import com.github.jmkgreen.morphia.Datastore;

@Named
public class PatentRepository {

	@Inject
	private Datastore ds;
	
	public void savePatentToDatabase(Iterator<Patent> patents,Project project){
		
		List<Patent> patentsCache = new ArrayList<Patent>();
		int cont = 0;
		while(patents.hasNext()){
			
			Patent p = patents.next();
			p.setProject(project);
			
			if(!patentExistsForProject(p, project)){				
				project.getPatents().add(p);
				patentsCache.add(p);
				cont++;
			}
			if(cont >= 30){
				ds.save(patentsCache);
				ds.save(project);
				patentsCache.clear();
				cont = 0;
			}
		}				
		if(cont > 0){
			ds.save(patentsCache);
			ds.save(project);
			patentsCache.clear();
		}
		
		
	}
	
	public boolean patentExistsForProject(Patent patent,Project project){		
		return ds.find(Patent.class)
					.field("publicationNumber").equal(patent.getPublicationNumber())
					.field("project").equal(project)
					.countAll() > 0;			
	}
}
