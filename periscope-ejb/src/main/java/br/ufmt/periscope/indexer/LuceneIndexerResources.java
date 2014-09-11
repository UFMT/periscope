package br.ufmt.periscope.indexer;

import br.ufmt.periscope.bean.SeedBean;
import br.ufmt.periscope.indexer.resources.analysis.CommonDescriptorsSet;
import br.ufmt.periscope.indexer.resources.analysis.PatenteeAnalyzer;
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
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

@Named
public class LuceneIndexerResources {

    private @Inject
    Datastore ds;

    private @Inject
    Project currentProject;

    @Produces
    public Version version = Version.LUCENE_47;

    @Produces
    public IndexReader getReader(Directory dir) {
        try {
            return DirectoryReader.open(dir);
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

//    @Produces
//    private CommonDescriptorsSet getCommonDescriptorSet(){
//        System.out.println("Produzindo CommonDescriptorSet");
//        return new CommonDescriptorsSet("");
//    }
//
//    @Produces
//    private Analyzer getAnalyzer() {
////        //return new StandardAnalyzer(Version.LUCENE_47);
//        System.out.println("Produzindo Analyzer");
//        return new PatenteeAnalyzer(Version.LUCENE_47);
//    }
    @Produces
    private IndexWriterConfig getIndexConfig(Analyzer analyzer) {
        //return new IndexWriterConfig(Version.LUCENE_47, analyzer);
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_47, analyzer);
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
//        System.out.println(SeedBean.PERISCOPE_DIR);
//        System.out.println("Titulo do projeto: " + currentProject.getTitle());
        try {
            dir = FSDirectory.open(new File(SeedBean.PERISCOPE_DIR), new SimpleFSLockFactory());
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
            Logger.getLogger(LuceneIndexerResources.class
                    .getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void disposesReader(@Disposes IndexReader reader) {
        try {
            reader.close();

        } catch (IOException ex) {
            Logger.getLogger(LuceneIndexerResources.class
                    .getName()).log(Level.SEVERE, null, ex);
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
