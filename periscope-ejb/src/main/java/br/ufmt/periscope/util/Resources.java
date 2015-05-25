package br.ufmt.periscope.util;

import java.net.UnknownHostException;
import java.util.logging.Logger;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Singleton;

import com.github.jmkgreen.morphia.Datastore;
import com.github.jmkgreen.morphia.Morphia;
import com.github.jmkgreen.morphia.mapping.lazy.DatastoreHolder;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.gridfs.GridFS;

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

    @Produces
    public GridFS produceFs(Logger log) {
        try {
            Mongo mongo = new Mongo("localhost", 27017);
            DB db = mongo.getDB("Periscope");
            GridFS fs = new GridFS(db);
            return fs;
        } catch (Exception e) {
            log.throwing(Resources.class.getName(), "produceFs", e);
        }
        return null;
    }
}
