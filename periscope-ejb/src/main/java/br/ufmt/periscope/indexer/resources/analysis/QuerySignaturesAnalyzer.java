package br.ufmt.periscope.indexer.resources.analysis;

import javax.inject.Inject;
import javax.inject.Named;

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
 *
 *
 * @author mattyws
 */
@Named
public class QuerySignaturesAnalyzer extends Analyzer {

    @Inject
    private CommonDescriptorsSet descriptorSet;
    public Version matchVersion = Version.LATEST;

    public QuerySignaturesAnalyzer() {
    }

    
    @Override
    protected TokenStreamComponents createComponents(String field) {
        // Tokenizes the string by withespace
        Tokenizer source = new WhitespaceTokenizer();
        TokenStream sink = null;
//        // Break the withespace in pattern and turn in two tokens, one with
//        // the acronym and other with the condesed name
//        sink = new CondenseTokenFilter(
//                // Removes the Common Descriptors of the company
//                new CommonDescriptorsTokenFilter(
//                        // Remove the English stopwords
//                        new StopFilter(matchVersion,
//                                // Pass the chars to their ASCII aquivalent
//                                new ASCIIFoldingFilter(
//                                        // Normalize the string
//                                        new LowerCaseFilter(matchVersion, source)), StandardAnalyzer.STOP_WORDS_SET), descriptorSet));

        LowerCaseFilter lowerCaseFilter = new LowerCaseFilter(source);
        
        ASCIIFoldingFilter aSCIIFoldingFilter = new ASCIIFoldingFilter(lowerCaseFilter);
        
//        
        StopFilter stopFilter = new StopFilter(aSCIIFoldingFilter, StandardAnalyzer.STOP_WORDS_SET);
        
//        
        CommonDescriptorsTokenFilter commonDescriptorsTokenFilter = new CommonDescriptorsTokenFilter(stopFilter, descriptorSet);
        
//        
        sink = new QuerySignaturesTokenFilter(commonDescriptorsTokenFilter);

//        return new TokenStreamComponents(source, commonDescriptorsTokenFilter);
        return new TokenStreamComponents(source, sink);
        
    }

}
