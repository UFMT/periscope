package br.ufmt.periscope.indexer;

import br.ufmt.periscope.model.Project;
import java.io.IOException;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.SimpleFSLockFactory;
import org.apache.lucene.util.Version;
import com.github.jmkgreen.morphia.Datastore;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

@Named
public class LuceneIndexerResources {

    private @Inject
    Datastore ds;
    
    private @Inject Project currentProject; 
 
    @Produces
    public IndexReader getReader(Directory dir) {
        try {
            return IndexReader.open(dir);
        } catch (CorruptIndexException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

//	@Produces
//	public IndexSearcher getSearcher(IndexReader reader){		
//		return new IndexSearcher(reader);	
//	}
    @Produces
    public IndexWriter getIndexWriter(Directory dir, IndexWriterConfig config) {
        IndexWriter iw = null;
        try {
            iw = new IndexWriter(dir, config);
        } catch (LockObtainFailedException ex) {
            Logger.getLogger(LuceneIndexerResources.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(LuceneIndexerResources.class.getName()).log(Level.SEVERE, null, ex);
        }
        return iw;
        
    }

    @Produces
    private Analyzer getAnalyzer() {
        //return new StandardAnalyzer(Version.LUCENE_36);
        return new PatenteeAnalyzer(Version.LUCENE_36);
    }

    @Produces
    private IndexWriterConfig getIndexConfig(Analyzer analyzer) {
        //return new IndexWriterConfig(Version.LUCENE_36, analyzer);
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_36, analyzer);
        return config;
    }

//   // @Produces
//    public Directory getMongoDBLuceneDirectory() {
//        DBCollection dbCollection = ds.getDB().getCollection("PatentIndex");
//
//        // serializers + map-store
//        DBObjectSerializer<String> keySerializer = new SimpleFieldDBObjectSerializer<String>("key");
//        DBObjectSerializer<MapDirectoryEntry> valueSerializer = new MapDirectoryEntrySerializer("value");
//        ConcurrentMap<String, MapDirectoryEntry> store = new MongoConcurrentMap<String, MapDirectoryEntry>(dbCollection, keySerializer, valueSerializer);
//
//        // lucene directory	
//        Directory dir;
//        try {
//            dir = new MapDirectory(store);
//        } catch (IOException e) {
//            e.printStackTrace();
//            return null;
//        }
//        return dir;
//
//    }

    @Produces
    public Directory getLocalLuceneDirectory() {
        Directory dir = null;
        try {
            System.out.println("Titulo do projeto: "+currentProject.getTitle());
            dir = FSDirectory.open(new File(System.getProperty("user.home")+"/periscope"), new SimpleFSLockFactory());
        } catch (IOException ex) {
            Logger.getLogger(LuceneIndexerResources.class.getName()).log(Level.SEVERE, null, ex);
        }
        return dir;

    }

    public void disposesWriter(@Disposes IndexWriter writer) {
        
        try {
            writer.deleteUnusedFiles();
            writer.commit();
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(LuceneIndexerResources.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

    public void disposesReader(@Disposes IndexReader reader) {
        try {
            reader.close();
        } catch (IOException ex) {
            Logger.getLogger(LuceneIndexerResources.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

//    public void disposesDirectory(@Disposes Directory dir) {
//        try {
//            dir.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

//	public void disposesSearcher(@Disposes IndexSearcher searcher){
//		try {
//			System.out.println("Disposing searcher");
//			searcher.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}	
}
