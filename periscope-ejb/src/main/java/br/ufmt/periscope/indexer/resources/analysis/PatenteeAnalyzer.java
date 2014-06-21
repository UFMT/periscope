package br.ufmt.periscope.indexer.resources.analysis;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;

/**
 * The PatenteeAnalyzer class will make the patentee names pre-processing, the
 * operations is : Normalize (pass the characters to lowercase); Pass the chars
 * to your ASCII equivalents; Removes the English Stopwords; Removes the Common
 * Descriptors of the names; Condense the name, removing all the withespaces;
 * Create the Acronym for the name
 *
 * @author mattyws
 */
public class PatenteeAnalyzer extends Analyzer {

    public Version matchVersion;
    String[] stopWords = {""};

    public PatenteeAnalyzer(Version matchVersion) {
        this.matchVersion = matchVersion;
    }

    @Override
    protected TokenStreamComponents createComponents(String field, Reader reader) {
        // Tokenizes the string by withespace
        Tokenizer source = new WhitespaceTokenizer(Version.LUCENE_47, reader);

        // Break the withespace in pattern and turn in two tokens, one with
        // the acronym and other with the condesed name
        TokenStream sink = new WhitespaceTokenFilter(
                // Condense the name, generates the acronym (if exists)
                // and return a token with the follow pattern
                // "acronym condenseName"
                new CondenseTokenFilter(
                        // Removes the Common Descriptors of the company
                        new CommonDescriptorsTokenFilter(
                                // Remove the English stopwords
                                new StopFilter(matchVersion,
                                        // Pass the chars to their ASCII aquivalent
                                        new ASCIIFoldingFilter(
                                                // Normalize the string
                                                new LowerCaseFilter(matchVersion, source)), StandardAnalyzer.STOP_WORDS_SET))));

        return new TokenStreamComponents(source, sink);
    }

}
