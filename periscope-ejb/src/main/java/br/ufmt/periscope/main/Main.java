//package br.ufmt.periscope.main;
//
//import java.io.IOException;
//import java.net.UnknownHostException;
//import java.util.Arrays;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.List;
//import java.util.concurrent.ConcurrentMap;
//
//import org.apache.lucene.analysis.standard.StandardAnalyzer;
//import org.apache.lucene.document.Document;
//import org.apache.lucene.index.IndexReader;
//import org.apache.lucene.queryParser.ParseException;
//import org.apache.lucene.queryParser.QueryParser;
//import org.apache.lucene.search.IndexSearcher;
//import org.apache.lucene.search.Query;
//import org.apache.lucene.search.ScoreDoc;
//import org.apache.lucene.search.TopScoreDocCollector;
//import org.apache.lucene.store.Directory;
//import org.apache.lucene.util.Version;
//
//import br.ufmt.periscope.enumerated.ClassificationType;
//import br.ufmt.periscope.model.Applicant;
//import br.ufmt.periscope.model.Classification;
//import br.ufmt.periscope.model.Patent;
//import br.ufmt.periscope.model.Project;
//import br.ufmt.periscope.model.User;
//import br.ufmt.periscope.model.UserLevel;
//
//import com.github.jmkgreen.morphia.Datastore;
//import com.github.jmkgreen.morphia.Morphia;
//import com.github.jmkgreen.morphia.mapping.Mapper;
//import com.github.jmkgreen.morphia.mapping.cache.EntityCache;
//import com.github.mongoutils.collections.DBObjectSerializer;
//import com.github.mongoutils.collections.MongoConcurrentMap;
//import com.github.mongoutils.collections.SimpleFieldDBObjectSerializer;
//import com.github.mongoutils.lucene.MapDirectory;
//import com.github.mongoutils.lucene.MapDirectoryEntry;
//import com.github.mongoutils.lucene.MapDirectoryEntrySerializer;
//import com.mongodb.BasicDBList;
//import com.mongodb.BasicDBObject;
//import com.mongodb.DBCollection;
//import com.mongodb.DBCursor;
//import com.mongodb.DBObject;
//import com.mongodb.Mongo;
//
//public class Main {
//	
//	public static void main(String[] args) throws IOException, ParseException {
//		
////		List<User> users =Fixjure.listOf(User.class)
////					        		.from(YamlSource.newYamlResource("user-inicial.yaml"))
////					        		.create();
////		Iterator<User> it = users.iterator();
////		while (it.hasNext()) {
////			System.out.println(it.next().getUserLevel());
////		}
//
//		
//		Datastore ds = criaConexao();
////		//criaExemplos(ds);
////		for(Project p : ds.createQuery(Project.class).asList()){
////			System.out.println(p.getTitle());
////			System.out.println(p.getOwner().getFirstname());
////			//addPatents(p,ds);
////			System.out.println(p.getPatents().size());
////			for(Patent pat : p.getPatents()){
////				System.out.println(pat.getTitleSelect());
////				System.out.println(pat.getMainClassification().getValue());				
////			}
////			break;			
////		}
////		classifications(ds);	
//		
//		//search("apple computer",ds);
//		
////		BasicDBObject keys = new BasicDBObject();
////		keys.put("applicants", 1);
////		Iterator it = ds.getCollection(Patent.class).distinct("applicants",new BasicDBObject()).iterator();
////		//DBCursor it = ds.getCollection(Patent.class).find(new BasicDBObject(),keys);		
////		while(it.hasNext()){
////			System.out.println(it.next());			
////		}
////		//System.out.println(it.count());
//		
//		long init = System.currentTimeMillis();
//		DBCollection dbCollection = ds.getDB().getCollection("PatentTestIndex");
//
//		// serializers + map-store
//		DBObjectSerializer<String> keySerializer = new SimpleFieldDBObjectSerializer<String>("key");
//		DBObjectSerializer<MapDirectoryEntry> valueSerializer = new MapDirectoryEntrySerializer("value");
//		ConcurrentMap<String, MapDirectoryEntry> store = new MongoConcurrentMap<String, MapDirectoryEntry>(dbCollection, keySerializer, valueSerializer);
//
//		// lucene directory
//		Directory dir = new MapDirectory(store);
//		
//
//		StandardAnalyzer analyser = new StandardAnalyzer(Version.LUCENE_36);
////		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_36, analyser);
////
////	    IndexWriter writer = new IndexWriter(dir, config);
////	    Query query = new QueryParser(Version.LUCENE_36, "publicationNumber", analyser).parse("AU002008201540A8 20090305");
////	    writer.deleteDocuments(query);
////	    writer.close();	    
////	    
////	    for(Patent p : ds.find(Patent.class).asList()){
////	    	Document doc = new Document();	    
////	    	
////	    	doc.add(new Field("publicationNumber",p.getPublicationNumber(), Field.Store.YES, Field.Index.ANALYZED));	    	
////	    	doc.add(new Field("titleSelect",p.getTitleSelect(), Field.Store.YES, Field.Index.ANALYZED));
////	    	if(p.getAbstractSelect() != null)
////	    		doc.add(new Field("abstractSelect",p.getAbstractSelect(), Field.Store.YES, Field.Index.ANALYZED));
////	    	
////	    	for(Applicant pa : p.getApplicants()){
////	    		doc.add(new Field("applicant", pa.getName(), Field.Store.YES, Field.Index.ANALYZED));
////	    	}
////	    	
////	    	writer.addDocument(doc);
////	    }	    	    
////	    writer.close();
//
//		
//		IndexReader reader = IndexReader.open(dir);
//		IndexSearcher searcher = new IndexSearcher(reader);		
//		TopScoreDocCollector collector = TopScoreDocCollector.create(10, true);			
//		Query query = new QueryParser(Version.LUCENE_36, "applicant", analyser).parse("APLEa~ INC~ NOT \"APPLE INC\"");		
//		
//		
//		searcher.search(query, collector);		
//		
//		ScoreDoc[] hits = collector.topDocs().scoreDocs;
//	    
//	    System.out.println("Found " + hits.length + " hits.");
//	    for(int i=0;i<hits.length;++i) {	    	
//	    	
//	      int docId = hits[i].doc;
//	      Document d = searcher.doc(docId);	      
//	      System.out.println((i + 1) + ". " + d.get("publicationNumber") + "\t" );
//	      System.out.println((i + 1) + ". " + d.get("applicant") + "\t" + hits[i].score );
//	      Patent patent = ds.createQuery(Patent.class).field("publicationNumber").equal(d.get("publicationNumber")).get();
//	      System.out.println(Arrays.toString(patent.getApplicants().toArray()));
//	      
//	    }	
//	    searcher.close();
//	    System.out.println((System.currentTimeMillis() - init)+" ms");
//		
//	}
//	
//	public static void search(String name,Datastore ds){
//		BasicDBObject where = new BasicDBObject();		
//		String suffix = name.split("[ ]+",-2)[0];
//		name = name.replace(" ", "");
//		where.put("$where", " return this.applicants.some(function(pa) { " +
//							"return (LiquidMetal().score(pa.name,'"+suffix+"') > 0.9) " +
//								"|| (levenshtein(pa.name,'"+name+"') < ('"+name+"'.length*0.5)); });");
//		System.out.println(where.get("$where"));
//		DBCursor cursor = ds.getCollection(Patent.class)
//							.find(where, new BasicDBObject("applicants",1));
//		
//		HashMap<String,Applicant> applicantsMap = new HashMap<String,Applicant>();
//		Mapper mapper = ds.getMapper();
//		EntityCache ec = mapper.createEntityCache();
//		while(cursor.hasNext()){
//			
//			BasicDBList objList = (BasicDBList) cursor.next().get("applicants");
//			Iterator<Object> it = objList.iterator();
//			while(it.hasNext()){
//				Applicant pa = (Applicant) mapper.fromDBObject(Applicant.class,(DBObject) it.next(), ec);
//				applicantsMap.put(pa.getName(),pa);
//			}			
//		}
//		Iterator<Applicant> aps = applicantsMap.values().iterator();
//		while(aps.hasNext()){
//			System.out.println(aps.next().getName());
//		}
//	}
//		
//	public static void classifications(Datastore ds){
//		DBCursor cursor = ds.getCollection(Patent.class).find(null, new BasicDBObject("mainClassification",1));
//		//DBCursor cursor = ds.getCollection(Patent.class).find(null,new BasicDBObject("mainClassification",1),0,30);
//		EntityCache ec = ds.getMapper().createEntityCache();
//		
//		while(cursor.hasNext()){
//			
//			DBObject obj = cursor.next();			
//			System.out.println(obj);
//			Classification classification = (Classification) ds.getMapper().fromDBObject(Classification.class,(DBObject)obj.get("mainClassification"), ec);							
//			System.out.println(classification.getValue());
//		}
//		System.out.println(cursor.count());
//	}
//	public static void addPatents(Project project,Datastore ds){
//		List<Patent> patents = project.getPatents();
//		
//		Patent patent = new Patent();
//		patent.setProject(project);
//		patent.setPublicationNumber("BR12312313US");
//		patent.setAbstractSelect("Muito boa patente");
//		patent.setTitleSelect("Patente para criar buracos negros");
//		patent.setMainClassification(new Classification("A31B 1/02",ClassificationType.ICAI));
//		
//		patents.add(patent);
//		
//		ds.save(patent);
//		ds.save(project);
//		
//	}
//	
//	public static void criaExemplos(Datastore ds){
//		User user = new User();
//		user.setUsername("alvaro");
//		user.setFirstname("Alvaro");
//		user.setLastname("Viebrantz");
//		user.setEmail("alvarowolfx@gmail.com");
//		user.setPassword("blablabla");
//		user.setUserLevel(UserLevel.ADMIN);
//		
//		ds.save(user);
//		
//		Project p = new Project();
//		p.setOwner(user);
//		p.setTitle("Teste de patentes");
//		p.setIsPublic(false);
//		p.setDescription("Testando patentes no mongodb");
//		p.setCreatedAt(new Date());
//		p.setUpdateAt(new Date());
//		
//		ds.save(p);
//	}
//	
//	public static Datastore criaConexao(){
//		Morphia m = new Morphia();
//		Datastore ds = null;
//		try {			
//			ds = m.createDatastore(new Mongo(), "Periscope");
//			ds.ensureCaps();
//			ds.ensureIndexes();
//			m.mapPackage("br.ufmt.periscope.model");
//			return ds;
//		} catch (UnknownHostException e) {
//			e.printStackTrace();
//		}	
//		System.out.println("Erro =/");
//		return ds;
//		
//	}
//
//}
