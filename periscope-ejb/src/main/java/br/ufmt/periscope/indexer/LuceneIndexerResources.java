package br.ufmt.periscope.indexer;

import java.io.IOException;
import java.util.concurrent.ConcurrentMap;

import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;

import com.github.jmkgreen.morphia.Datastore;
import com.github.mongoutils.collections.DBObjectSerializer;
import com.github.mongoutils.collections.MongoConcurrentMap;
import com.github.mongoutils.collections.SimpleFieldDBObjectSerializer;
import com.github.mongoutils.lucene.MapDirectory;
import com.github.mongoutils.lucene.MapDirectoryEntry;
import com.github.mongoutils.lucene.MapDirectoryEntrySerializer;
import com.mongodb.DBCollection;

@Named
public class LuceneIndexerResources {

	private @Inject Datastore ds;
	
	@Produces
	public IndexReader getReader(Directory dir){
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
	public IndexWriter getIndexWriter(Directory dir,IndexWriterConfig config){
		try {
			return new IndexWriter(dir, config);
		} catch (CorruptIndexException e) {
			e.printStackTrace();
		} catch (LockObtainFailedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}		
		return null;
	}
	
	@Produces
	private StandardAnalyzer getAnalyzer(){				
		return new StandardAnalyzer(Version.LUCENE_36);
	}
	
	@Produces
	private IndexWriterConfig getIndexConfig(StandardAnalyzer analyzer){		
		//return new IndexWriterConfig(Version.LUCENE_36, analyzer);
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_36, analyzer);
		
		return config;
	}
	
	@Produces
	public Directory getMongoDBLuceneDirectory(){
		DBCollection dbCollection = ds.getDB().getCollection("PatentIndex");

		// serializers + map-store
		DBObjectSerializer<String> keySerializer = new SimpleFieldDBObjectSerializer<String>("key");
		DBObjectSerializer<MapDirectoryEntry> valueSerializer = new MapDirectoryEntrySerializer("value");
		ConcurrentMap<String, MapDirectoryEntry> store = new MongoConcurrentMap<String, MapDirectoryEntry>(dbCollection, keySerializer, valueSerializer);

		// lucene directory	
		Directory dir;
		try {
			dir = new MapDirectory(store);			
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return dir;
		
	}
	
	public void disposesWriter(@Disposes IndexWriter writer){
		try {
			System.out.println("Disposing Writer");
			writer.deleteUnusedFiles();		
			writer.commit();			
			writer.close();
		} catch (CorruptIndexException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void disposesReader(@Disposes IndexReader reader){
		try {
			System.out.println("Disposing Reader");
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
//	public void disposesSearcher(@Disposes IndexSearcher searcher){
//		try {
//			System.out.println("Disposing searcher");
//			searcher.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}	
	
}
