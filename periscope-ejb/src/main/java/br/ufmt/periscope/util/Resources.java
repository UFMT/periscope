package br.ufmt.periscope.util;

import java.net.UnknownHostException;
import java.util.logging.Logger;

import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.faces.bean.ManagedBean;
import javax.inject.Singleton;

import com.github.jmkgreen.morphia.Datastore;
import com.github.jmkgreen.morphia.Morphia;
import com.github.jmkgreen.morphia.mapping.lazy.DatastoreHolder;
import com.mongodb.Mongo;

/**
 * This class uses CDI to alias Java EE resources, such as the persistence
 * context, to CDI beans
 * 
 * <p>
 * Example injection on a managed bean field:
 * </p>
 * 
 * <pre>
 * &#064;Inject
 * private EntityManager em;
 * </pre>
 */
public class Resources {

	//@Produces
	//@PersistenceContext
	//private EntityManager em;
	
	@Singleton
	@Produces
	private Mongo createMongo() {
		try {			
			return new Mongo();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Produces
	private Morphia morphia = new Morphia();

	@Produces
	public Datastore mongoDs(Mongo mongo, Morphia morphia) {				
		Datastore ds = morphia.createDatastore(mongo, "Periscope");		
		ds.ensureCaps();
		ds.ensureIndexes();
		morphia.mapPackage("br.ufmt.periscope.model");		
		DatastoreHolder.getInstance().set(ds);
		return ds;
	}	
	
	@Produces
	public Logger produceLog(InjectionPoint injectionPoint) {
		return Logger.getLogger(injectionPoint.getMember().getDeclaringClass()
				.getName());
	}
}
