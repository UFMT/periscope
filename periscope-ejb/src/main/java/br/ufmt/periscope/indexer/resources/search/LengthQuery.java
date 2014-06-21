/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufmt.periscope.indexer.resources.search;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.index.FilteredTermsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.util.AttributeSource;

/**
 * A query that will return the results that is greater than the condesed name
 * if don't have a acronym, or if have a acronym, returns the name that starts
 * with the acronym and is greater than the condesed name
 * REMEMBER, pass to parameter the name that already pass through the
 * PatenteeAnalyzer
 * @author mattyws
 */
public class LengthQuery extends MultiTermQuery {

    private Term acronym;
    private Term name;

    /**
     * The constructor for the class
     * @param field field where the term will be searched
     * @param query the search text, already pass through the PatenteeAnalyzer
     */
    public LengthQuery(String field, String query) {
        super(field);
        // If contains a space, contains a acronym
        if (query.contains(" ")) {
            String[] tokens = query.split(" ");
            acronym = new Term(field, tokens[0]);
            name = new Term(field, tokens[1]);
        } else {
            acronym = null;
            name = new Term(field, query);
        }
    }

    @Override
    protected TermsEnum getTermsEnum(Terms terms, AttributeSource as) throws IOException {
        TermsEnum tenum = terms.iterator(null);
        if(name.bytes().length == 0) {
            return tenum;
        }
        return new LengthTermEnum(tenum, acronym, name);
    }

    @Override
    public String toString(String string) {
        return "";
    }
}
