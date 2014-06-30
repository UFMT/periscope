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
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;

import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.model.Inventor;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;
import java.util.logging.Level;
import javax.enterprise.context.RequestScoped;
import org.apache.lucene.document.TextField;

@Named
@RequestScoped
public class PatentIndexer {

    private @Inject
    IndexWriter writer;
    private @Inject
    IndexReader reader;
    private @Inject
    Analyzer analyzer;
    private @Inject
    Logger log;

    public void indexPatents(Iterable<Patent> patents) {
        Iterator<Patent> it = patents.iterator();
        while (it.hasNext()) {
            Patent p = it.next();
            if (p == null) {
                continue;
            }
            indexPatent(p);
        }
    }

    public void deleteIndexesForProject(Project project) {
        log.info("Deletando indices para o projeto " + project.getTitle());
        try {
            writer.deleteDocuments(new Term("project", project.getId().toString()));
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

        try {
            for (Applicant a : p.getApplicants()) {
                Document doc = new Document();
                doc.add(new TextField("id", p.getTitleSelect() + p.getPublicationNumber() + p.getProject().getId().toString() + String.valueOf(a.getName().hashCode()), Field.Store.YES));
                doc.add(new TextField("applicant", a.getName(), Field.Store.YES));
                doc.add(new TextField("project", p.getProject().getId().toString(), Field.Store.YES));
                writer.updateDocument(new Term(doc.get("id")), doc);
            }

            for (Inventor a : p.getInventors()) {
                Document doc = new Document();
                doc.add(new TextField("id", p.getTitleSelect() + p.getPublicationNumber() + p.getProject().getId().toString() + String.valueOf(a.getName().hashCode()), Field.Store.YES));
                doc.add(new TextField("inventor", a.getName(), Field.Store.YES));
                doc.add(new TextField("project", p.getProject().getId().toString(), Field.Store.YES));
                writer.updateDocument(new Term(doc.get("id")), doc);

            }
        } catch (IOException ex) {
            Logger.getLogger(PatentIndexer.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
