/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package br.ufmt.periscope.indexer.resources.search;

import java.io.IOException;
import org.apache.lucene.index.FilteredTermsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

/**
 *
 * @author mattyws
 */
public class FastJoinTermEnum extends FilteredTermsEnum {
    
    private BytesRef name;
    private FuzzyTokenSimilarity ts;

    public FastJoinTermEnum(TermsEnum tenum, Term name, FuzzyTokenSimilarity ts) {
        super(tenum);
        this.name = name.bytes();
        this.ts = ts;
        setInitialSeekTerm(this.name);
    }

    @Override
    protected AcceptStatus accept(BytesRef term) throws IOException {        
        if(ts.execute(this.name.utf8ToString(), term.utf8ToString()) != 0) {
            if(ts.fuzzyCosine() != 0 || ts.fuzzyDice() != 0 || ts.fuzzyJaccard() != 0){
                return AcceptStatus.YES;
            } else {
                return AcceptStatus.NO;
            }
        } else {
            return AcceptStatus.NO_AND_SEEK;
        }
    }
    
}
