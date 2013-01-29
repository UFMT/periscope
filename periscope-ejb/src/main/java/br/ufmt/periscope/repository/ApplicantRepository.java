package br.ufmt.periscope.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.util.Version;

import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;

import com.github.jmkgreen.morphia.Datastore;
import com.github.jmkgreen.morphia.mapping.Mapper;
import com.github.jmkgreen.morphia.mapping.cache.EntityCache;
import com.google.common.collect.HashMultiset;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

@Named
public class ApplicantRepository {

	private @Inject Datastore ds;
	private @Inject IndexSearcher searcher;
	private @Inject StandardAnalyzer analyzer;
	
	public List<Applicant> getApplicants(Project project){
		Map<String,Applicant> map = new HashMap<String,Applicant>();
		
		HashMultiset<String> bag = HashMultiset.create(); 
		
		BasicDBObject where = new BasicDBObject();		
		where.put("project.$id", project.getId());		
		where.put("applicants", new BasicDBObject("$exists", true));
		
		BasicDBObject keys = new BasicDBObject();
		keys.put("applicants",1);
								
		DBCursor cursor = ds.getCollection(Patent.class).find(where, keys);
		Mapper mapper = ds.getMapper();
		EntityCache ec = mapper.createEntityCache();
		while(cursor.hasNext()){
			
			BasicDBList objList = (BasicDBList) cursor.next().get("applicants");			
			Iterator<Object> itList = objList.iterator();
			while(itList.hasNext()){
				Applicant pa = (Applicant) mapper.fromDBObject(Applicant.class,(DBObject) itList.next(), ec);
				bag.add(pa.getName());
				pa.setDocumentCount(bag.count(pa.getName()));
				map.put(pa.getName(),pa);
			}								
						
		}
		return new ArrayList<Applicant>(map.values());
	}
	
	public Set<String> getApplicantSugestions(Project project,int top,String... names){
		
	    Set<String> results = new HashSet<String>();							
		try {
			StringBuilder queryBuilder = new StringBuilder();			
			for(String name : names){
				String[] terms = name.split(" ",-2);
				for(String term : terms){
					//if(term.length() >= 4){		
						queryBuilder.append(term+"~ ");
						queryBuilder.append(term+"* ");
					//}
				}	
				
				//queryBuilder.append("NOT \""+name+"\" ");	
				queryBuilder.append("\""+name+"\"~10 ");
			}		
						
			TopScoreDocCollector collector = TopScoreDocCollector.create(1000, true);			
			BooleanQuery bq = new BooleanQuery();			
			Query queryPa = new QueryParser(Version.LUCENE_36, "applicant", analyzer)
											.parse(queryBuilder.toString());
			queryPa.setBoost(10f);		
			
			Query queryProject = new QueryParser(Version.LUCENE_36, "project", analyzer)
											.parse(project.getId().toString());
			queryProject.setBoost(0.1f);					
			
			bq.add(queryPa, Occur.MUST);
			bq.add(queryProject,Occur.MUST);
			System.out.println(bq);
			
			searcher.search(bq, collector);						    			
			
			ScoreDoc[] hits = collector.topDocs().scoreDocs;
		    System.out.println("Found " + hits.length + " hits.");
		    for(int i=0;i<hits.length;++i) {	    			    	
		      int docId = hits[i].doc;
		      Document d = searcher.doc(docId);	      		      
		      System.out.println((i + 1) + ". " + d.get("applicant") + "\t" + hits[i].score );		      
		      results.add(d.get("applicant"));
		      
		      if(results.size() == top ) break;
		    }
		    for(String name : names){
		    	results.remove(name);
		    }
		    
		    
		    return results;
		    		
		} catch (CorruptIndexException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return results;		
				
		
	}
}
