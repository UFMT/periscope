package br.ufmt.periscope.main;

import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

public class MainTCC {

	public static void main(String[] args) throws CorruptIndexException, IOException, ParseException {
		
		Directory index = new RAMDirectory();
		StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_36);
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_36,analyzer);

		IndexWriter writer = new IndexWriter(index, config);
		addDocument(writer,"Touch screen liquid crystal display");
		addDocument(writer,"Portable electronic device for imaged-based browsing of contacts");
		addDocument(writer,"Docking station for hand held electronic devices");
		addDocument(writer,"Insert for adapting hand held electronic devices to a docking station");
		addDocument(writer,"Accessory authentication for electronic devices");
		writer.commit();
		
		String queryString = "eletro~ device~";

		Query query = new QueryParser(Version.LUCENE_36,"title",analyzer).parse(queryString);

		IndexReader reader = IndexReader.open(index);	
		IndexSearcher searcher = new IndexSearcher(reader);

		TopScoreDocCollector collector = TopScoreDocCollector.create(10, true);
		searcher.search(query, collector);
		
		ScoreDoc[] hits = collector.topDocs().scoreDocs;			
		System.out.println("Encontrado " + hits.length + " hits.");
		for(int i=0;i<hits.length;++i) {	    			    	
		  int docId = hits[i].doc;
		  Document d = searcher.doc(docId);	      		      
		  System.out.println((i + 1) + ". " + d.get("title") + "\t" + hits[i].score );		      
		}

		writer.close();
		searcher.close();

	}
	

	private static void addDocument(IndexWriter writer,String title) throws CorruptIndexException, IOException{

		Document doc = new Document();
		doc.add(new Field("title",title,Field.Store.YES,Field.Index.ANALYZED));
		writer.addDocument(doc);
		
	}
	
}
