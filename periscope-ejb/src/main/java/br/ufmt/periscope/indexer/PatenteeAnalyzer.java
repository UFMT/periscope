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
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import org.apache.lucene.util.Version;

public class PatenteeAnalyzer extends Analyzer {

    public Version matchVersion;
    String[] stopWords = {""};

    public PatenteeAnalyzer(Version matchVersion) {
        this.matchVersion = matchVersion;
    }

    @Override
    protected TokenStreamComponents createComponents(String string, Reader reader) {
        Tokenizer source = new WhitespaceTokenizer(Version.LUCENE_36, reader);

        /*
         * This part will : 
         * Normalize all characters to lowercase 
         * Turn accented letters to they ASCII equivalents 
         * Remove all the stopwords from source
         * Condense the string
         * TODO: Remove the punctuation characters
         */
        TokenStream sink = new CondenseFilter(new StopFilter(matchVersion, new ASCIIFoldingFilter(
                new LowerCaseFilter(matchVersion, source)), StandardAnalyzer.STOP_WORDS_SET));

        return new TokenStreamComponents(source,sink);
    }

}
