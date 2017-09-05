/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufmt.periscope.indexer.resources.search;

import br.ufmt.periscope.indexer.LuceneIndexerResources;;
import br.ufmt.periscope.indexer.resources.analysis.FastJoinAnalyzer;
import br.ufmt.periscope.indexer.resources.analysis.QuerySignaturesAnalyzer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;

/**
 *
 * @author horgun
 */

@Named
public class FuzzyTokenSimilaritySearch {
    private final float editSimilarityThreshold = 0.75f;
    private final float tokenThreshold = 0.7f;
    private @Inject
    FastJoinAnalyzer fastJoinAnalyzer;
    private @Inject
    QuerySignaturesAnalyzer querySignaturesAnalyzer;
    private @Inject
    LuceneIndexerResources resources;
    
    public FuzzyTokenSimilaritySearch(){
    }
    
    public String getAnalyzed(String field, String string){
        String s = "";
        try {
            TokenStream ts = fastJoinAnalyzer.tokenStream(field, string);
            CharTermAttribute attr = ts.getAttribute(CharTermAttribute.class);
            ts.reset();

            while (ts.incrementToken()) {
                s += attr.toString() + " ";
            }
            s = s.trim();
            ts.close();
        } catch (IOException ex) {
            Logger.getLogger(FuzzyTokenSimilaritySearch.class.getName()).log(Level.SEVERE, null, ex);
        }
        return s;
    }
    
    public List<Document> search(String field, String project, String value, int top){
        TermQuery projQuery = new TermQuery(new Term("project", project));
        BooleanQuery.Builder bq = new BooleanQuery.Builder();
        
        try {
            if (querySignaturesAnalyzer == null){
            }
            TokenStream ts = querySignaturesAnalyzer.tokenStream(field, value);
            CharTermAttribute attr = ts.getAttribute(CharTermAttribute.class);
            ts.reset();
            
            while (ts.incrementToken()) {
//                System.out.println(attr.toString());
                bq.add(new BooleanQuery.Builder().add(new TermQuery(new Term(field, attr.toString())), BooleanClause.Occur.MUST).add(projQuery, BooleanClause.Occur.MUST).build(), BooleanClause.Occur.SHOULD);
            }
            ts.close();
        } catch (IOException ex) {
            Logger.getLogger(FuzzyTokenSimilaritySearch.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        IndexReader reader = resources.getReader();
        IndexSearcher buscador = new IndexSearcher(reader);
        
        List<Document> docs = new ArrayList();
        try {
            
            ScoreDoc[] resultados = buscador.search(bq.build(), top).scoreDocs;
            for (ScoreDoc resultado : resultados){
                docs.add(buscador.doc(resultado.doc));
            }
            resources.closeReader(reader);
        } catch (IOException ex) {
            Logger.getLogger(FuzzyTokenSimilaritySearch.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        List<Document> finalResults = new ArrayList();
        String T1[] = getAnalyzed(field, value).split(" ");
        System.out.println("Retornados: " + docs.size());
        for (Document doc : docs){
//            System.out.println("doc: " + doc.get(field));
            float upperBound = 0;
            String T2[] = doc.get("tokens").split(" ");
            for (int i = 0; i < T1.length; i++){
                for (int j = 0; j < T2.length; j++){
                    float distTokens = 1 - (new DamerauLevenshteinAlgorithm(1, 1, 1, 1).execute(T1[i], T2[j]) * 1.0f/Math.max(T1[i].length(), T2[j].length()));
//                    System.out.println("DT(" + T1[i] + ", " + T2[j] + ") = " + distTokens);
                    if (distTokens > editSimilarityThreshold)
                        upperBound += distTokens;
                }
            }
//            System.out.println("UB: " + upperBound);
            float fuzzyJaccard = Math.abs(upperBound) / (T1.length + T2.length - Math.abs(upperBound));
//            System.out.println("FJ: " + fuzzyJaccard);
            if (fuzzyJaccard >= tokenThreshold){
                finalResults.add(doc);
            }
        }
//        System.out.println("Quant: " + docs.size());
        return finalResults;
    }

    
    
}
