package br.ufmt.periscope.indexer;

import br.ufmt.periscope.indexer.resources.analysis.PatenteeAnalyzer;
import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.model.Inventor;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.repository.ApplicantRepository;
import br.ufmt.periscope.repository.InventorRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;

@Named
@RequestScoped
public class PatentIndexer {

    private @Inject
    IndexWriter writer;
    private @Inject
    IndexReader reader;
    private @Inject
    PatenteeAnalyzer analyzer;
    private @Inject
    ApplicantRepository paRepo;
    private @Inject
    InventorRepository inRepo;
    private @Inject
    Logger log;

    public void indexPatents(List<Patent> patents, Project project) {
        Set<String> applicants = new HashSet<String>();
        Set<String> inventors = new HashSet<String>();
        for (Patent patent : patents) {
            for (Applicant pa : patent.getApplicants()) {
                applicants.add(pa.getName());
            }
            for (Inventor inv : patent.getInventors()) {
                inventors.add(inv.getName());
            }
        }
        index(new ArrayList<String>(applicants), new ArrayList<String>(inventors), project);
    }

    public void deleteIndexesForProject(Project project) {
//        log.info("Deletando indices para o projeto " + project.getTitle());
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

    public void index(List<String> pas, List<String> invs, Project project) {
        try {
            if (pas != null) {
                for (String a : pas) {
                    Document doc = new Document();
                    doc.add(new TextField("id", project.getId().toString() + String.valueOf(a.hashCode()), Field.Store.YES));
                    doc.add(new TextField("applicant", a, Field.Store.YES));
                    doc.add(new TextField("project", project.getId().toString(), Field.Store.YES));
                    writer.deleteDocuments(new Term("id", doc.get("id")));
                    writer.addDocument(doc);
                }
            }
            if (invs != null) {
                for (String a : invs) {
                    Document doc = new Document();
                    doc.add(new TextField("id", project.getId().toString() + String.valueOf(a.hashCode()), Field.Store.YES));
                    doc.add(new TextField("inventor", a, Field.Store.YES));
                    doc.add(new TextField("project", project.getId().toString(), Field.Store.YES));
                    writer.deleteDocuments(new Term("id", doc.get("id")));
                    writer.addDocument(doc);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(PatentIndexer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void reindex(Project project) {
        deleteIndexesForProject(project);
        List<String> pas = paRepo.getApplicants(project, "");
        List<String> invs = inRepo.getInventors(project, "");
        index(pas, invs, project);
    }

    public void indexRule(List<String> pas, String pa, List<String> invs, String inv, Project project) {
        try {
            if (pas != null) {
                for (String a : pas) {
                    Document doc = new Document();
                    doc.add(new TextField("id", project.getId().toString() + String.valueOf(a.hashCode()), Field.Store.YES));
                    writer.deleteDocuments(new Term("id", doc.get("id")));
                }
                Document doc = new Document();
                doc.add(new TextField("id", project.getId().toString() + String.valueOf(pa.hashCode()), Field.Store.YES));
                doc.add(new TextField("applicant", pa, Field.Store.YES));
                doc.add(new TextField("project", project.getId().toString(), Field.Store.YES));
                writer.deleteDocuments(new Term("id", doc.get("id")));
                writer.addDocument(doc);
            }
            if (invs != null) {
                for (String a : invs) {
                    Document doc = new Document();
                    doc.add(new TextField("id", project.getId().toString() + String.valueOf(a.hashCode()), Field.Store.YES));
                    writer.deleteDocuments(new Term("id", doc.get("id")));
                }
                Document doc = new Document();
                doc.add(new TextField("id", project.getId().toString() + String.valueOf(inv.hashCode()), Field.Store.YES));
                doc.add(new TextField("inventor", inv, Field.Store.YES));
                doc.add(new TextField("project", project.getId().toString(), Field.Store.YES));
                writer.deleteDocuments(new Term("id", doc.get("id")));
                writer.addDocument(doc);
            }
        } catch (IOException ex) {
            Logger.getLogger(PatentIndexer.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void indexPatent(Patent p) {
        Long in = System.currentTimeMillis();
        List<String> pas = new ArrayList<String>();
        List<String> invs = new ArrayList<String>();
        if (p.getApplicants() != null) {
            for (Applicant a : p.getApplicants()) {
                pas.add(a.getName());
            }
        }
        in = System.currentTimeMillis();
        if (p.getInventors() != null) {
            for (Inventor a : p.getInventors()) {
                invs.add(a.getName());
            }
        }
        index(pas, invs, p.getProject());
    }
}
