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
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.StringHelper;

/**
 * This class will compare the terms based on the logic of LengthQuery
 * @author mattyws
 */
public class LengthTermEnum extends FilteredTermsEnum {

    private BytesRef acronym = null;
    private BytesRef name;

    public LengthTermEnum(TermsEnum tenum, Term acronym, Term name) {
        // Setting the terms for the search
        super(tenum);
        // If the acronym come as null, so, do not have an acronym
        if (acronym == null) {
            this.name = name.bytes();
            setInitialSeekTerm(this.name);
        } else {
            // Have a acronym
            this.acronym = acronym.bytes();
            this.name = name.bytes();
            setInitialSeekTerm(this.acronym);
        }
    }

    @Override
    protected AcceptStatus accept(BytesRef br) throws IOException {
        // If we don't have an acronym, we will not use startsWith a acronym
        if (acronym == null) {
            if (name.length + (name.length * 0.45) < br.length) {
                return AcceptStatus.YES;
            } else {
                return AcceptStatus.NO;
            }
        } else {
            // But if we have, let's return that startsWith the acronym
            if (StringHelper.startsWith(br, acronym)) {
                if (name.length + (name.length * 0.45) < br.length) {
                    return AcceptStatus.YES;
                } else {
                    return AcceptStatus.NO;
                }
            } else {
                return AcceptStatus.NO;
            }
        }
    }
}
