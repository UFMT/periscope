package br.ufmt.periscope.indexer;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;

import org.apache.lucene.util.Version;

public class PatenteeAnalyzer extends Analyzer {

    public Version matchVersion;
    String[] stopWords = {""};

    public PatenteeAnalyzer(Version matchVersion) {
        this.matchVersion = matchVersion;
    }

//    @Override
//    public TokenStream tokenStream(String field, Reader reader) {
//        // Get the stopwords set
//        CharArraySet stopWords = this.stopWords();
//        // Iniciate a WhitespaceTokenizer, to remove the stop words in next step
//        Tokenizer source = new WhitespaceTokenizer(Version.LUCENE_36, reader);
//
//        /*
//         * This part will : 
//         * Normalize all characters to lowercase 
//         * Turn accented letters to they ASCII equivalents 
//         * Remove all the stopwords from source
//         * Condense the string
//         * TODO: Remove the punctuation characters
//         */
//        TokenStream sink = new CondenseFilter(new StopFilter(matchVersion, new ASCIIFoldingFilter(
//                new LowerCaseFilter(matchVersion, source)), stopWords));
//        
//        return sink;
//    }

    /**
     * Create the stop words set
     *
     * TODO: Change the source, beacause is a String vector from this class
     *
     * @return a CharArraySet from stop words
     */
    private CharArraySet stopWords() {
        // Instanciate the CharArraySet
        CharArraySet cstopWords = new CharArraySet(Version.LUCENE_CURRENT, 2,
                true);
        // Get from the souce, here is a Array of strings
        for (String s : stopWords) {
            cstopWords.add(s);
        }
        // Return it
        return cstopWords;
    }

    @Override
    protected TokenStreamComponents createComponents(String string, Reader reader) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
