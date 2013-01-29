package br.ufmt.periscope.indexer;

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;

import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.model.Inventor;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;

@Named
public class PatentIndexer {

	private @Inject IndexWriter writer;
	private @Inject Analyzer analyzer;
	private @Inject Logger log;
			
	public void indexPatents(Iterable<Patent> patents){		
		Iterator<Patent> it = patents.iterator();
		while(it.hasNext()){
			indexPatent(it.next());
		}		
	}	
	
	public void deleteIndexesForProject(Project project){
		log.info("Deletando indices para o projeto " +project.getTitle());
		try {
			writer.deleteDocuments(new Term("project",project.getId().toString()));
			log.info("Indices deletados com sucesso");
			return;
		} catch (CorruptIndexException e) {
			e.printStackTrace();
		} catch (IOException e) {		
			e.printStackTrace();
		}	
		log.info("Ocorreu algum erro deletando os indices.");
	}
	
	private void indexPatent(Patent p) {
		Document doc = new Document();	    
    			
		doc.add(new Field("id",p.getPublicationNumber()+p.getProject().getId().toString(), Field.Store.YES, Field.Index.ANALYZED));
    	doc.add(new Field("publicationNumber",p.getPublicationNumber(), Field.Store.YES, Field.Index.ANALYZED));
    	doc.add(new Field("project",p.getProject().getId().toString(), Field.Store.YES, Field.Index.ANALYZED));
    	doc.add(new Field("titleSelect",p.getTitleSelect(), Field.Store.YES, Field.Index.ANALYZED));
    	if(p.getAbstractSelect() != null)
    		doc.add(new Field("abstractSelect",p.getAbstractSelect(), Field.Store.YES, Field.Index.ANALYZED));
    	
    	for(Applicant pa : p.getApplicants()){
    		doc.add(new Field("applicant", pa.getName(), Field.Store.YES, Field.Index.ANALYZED));
    	}
    	
    	for(Inventor pi : p.getInventors()){
    		doc.add(new Field("inventor", pi.getName(), Field.Store.YES, Field.Index.ANALYZED));
    	}
    	
    	try {
    		    		
    	    writer.deleteDocuments(new Term("id",doc.get("id")));    	    
			writer.addDocument(doc);
			//writer.commit();
			
		} catch (CorruptIndexException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
