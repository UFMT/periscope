package br.ufmt.periscope.indexer.resources.analysis;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

/**
 * CondenseFilter will put all tokens together and will create a acronym if :
 * The token has a lenght more then 1; The acronym has a lenght more then 2
 *
 * @author mattyws
 *
 */
public class CondenseTokenFilter extends TokenFilter {

    private final StringBuilder sb = new StringBuilder();
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
    private boolean consumed; // true if we already consumed

    /**
     * The default constructor of TokenFilter
     * @param input the input TokenStream
     */
    protected CondenseTokenFilter(TokenStream input) {
        super(input);
    }

    @Override
    public final boolean incrementToken() throws IOException {
        if (consumed) {
            return false; // don't call input.incrementToken() after it returns
            // false
        }
        consumed = true;

        int startOffset = 0;
        int endOffset = 0;

        boolean found = false; // true if we actually consumed any tokens
        // Lets consume all the tokens
        while (input.incrementToken()) {
            if (!found) {                
                startOffset = offsetAtt.startOffset();
                found = true;
            }            
            sb.append(termAtt);
            // The '_' is just to create the acronym after
            sb.append("_");
            endOffset = offsetAtt.endOffset();
        }
        // If we found any token
        if (found) {
            // Clear the attributes to haven o error
            clearAttributes();
            String name = sb.toString().replaceAll("[^A-Za-z0-9_]", "");
            if (name.length() > 0) {
                name = name.trim();                
                String[] words = name.split("_");
                name = "";
                for(String word : words) {
                    if(word.trim().length() > 0) {
                        name += word + '_';
                    }
                }
                if(name.length() > 0)
                    name = name.substring(0, name.length()-1);
                else
                    return false;
                sb.delete(0, sb.length());
                sb.append(name);
                termAtt.setEmpty();
                // Just append the acronym if the length is more than 2
                termAtt.append(name);
                offsetAtt.setOffset(startOffset, endOffset);
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        sb.setLength(0);
        consumed = false;
    }

}
