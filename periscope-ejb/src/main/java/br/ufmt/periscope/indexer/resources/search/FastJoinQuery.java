/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package br.ufmt.periscope.indexer.resources.search;

import java.io.IOException;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.util.AttributeSource;

/**
 *
 * @author mattyws
 */
public class FastJoinQuery extends MultiTermQuery{
    
    private Term name;
    private FuzzyTokenSimilarity ts;

    public FastJoinQuery(String field, String query, float tokenThreshold, float editThreshold) {
        super(field);
        ts = new FuzzyTokenSimilarity(tokenThreshold, editThreshold);
        if (query.contains(" ")) {
            String[] tokens = query.split(" ");
            name = new Term(field, tokens[1]);
        } else {
            name = new Term(field, query);
        }
    }

    @Override
    protected TermsEnum getTermsEnum(Terms terms, AttributeSource atts) throws IOException {
        TermsEnum tenum = terms.iterator(null);        
        return new FastJoinTermEnum(tenum, name, this.ts);
    }

    @Override
    public String toString(String field) {
        return "";
    }
    
}
