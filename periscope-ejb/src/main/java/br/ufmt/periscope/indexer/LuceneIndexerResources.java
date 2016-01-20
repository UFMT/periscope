package br.ufmt.periscope.indexer;

import br.ufmt.periscope.bean.SeedBean;
import br.ufmt.periscope.indexer.resources.analysis.FastJoinAnalyzer;
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
import javax.faces.bean.ViewScoped;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

@Named
public class LuceneIndexerResources {

    
    private @Inject FastJoinAnalyzer analyzer;    

    public IndexReader getReader() {
        IndexReader reader=null;
        System.out.println("Get reader");
        Directory dir = this.getLocalLuceneDirectory();
        if (dir != null) {
            try {                
                reader = DirectoryReader.open(dir);
            } catch (CorruptIndexException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return reader;
    }

    public IndexWriter getIndexWriter() {
        Directory dir = this.getLocalLuceneDirectory();
        IndexWriterConfig config = this.getIndexConfig();
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

    private IndexWriterConfig getIndexConfig() {
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_47, analyzer);
        return config;
    }

    
    public Directory getLocalLuceneDirectory() {
        Directory dir = null;
        try {
            dir = FSDirectory.open(new File(SeedBean.PERISCOPE_DIR), new SimpleFSLockFactory());
        } catch (IOException ex) {
            Logger.getLogger(LuceneIndexerResources.class.getName()).log(Level.SEVERE, null, ex);
        }
        return dir;

    }
    
    public void closeWriter(IndexWriter writer) {
         try {
            writer.deleteUnusedFiles();
            writer.commit();
            writer.close();

        } catch (IOException ex) {
            Logger.getLogger(LuceneIndexerResources.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void closeReader(IndexReader reader) {
        try {
            System.out.println("closing reader");
            reader.close();
        } catch (IOException ex) {
            Logger.getLogger(LuceneIndexerResources.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

 }
