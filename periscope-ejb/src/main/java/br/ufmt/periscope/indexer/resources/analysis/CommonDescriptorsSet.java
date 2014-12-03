package br.ufmt.periscope.indexer.resources.analysis;

import br.ufmt.periscope.indexer.LuceneIndexerResources;
import com.github.jmkgreen.morphia.Datastore;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;

/**
 * This class will contains the commons company descriptors Now, it's just use a
 * ArrayList, but this may change in the future
 *
 * @author mattyws
 */
@Named
public class CommonDescriptorsSet {

    private @Inject
    Datastore ds;    

    /**
     * Constructor for Lucene based CommonDescriptorsSet
     */
    public CommonDescriptorsSet() {
    }

    
    public boolean contains(String descriptor) {       
        List<CommonDescriptor> descriptorSet = ds
                .find(CommonDescriptor.class)
                .field("_id").equal(descriptor)
                .asList();

        if (descriptorSet != null && !descriptorSet.isEmpty()) {
            return true;
        }
        return false;
    }

}
