package br.ufmt.periscope.indexer.resources.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

/**
 * The token filter that will remove the Common company descriptors
 *
 * @author mattyws
 */
public class CommonDescriptorsTokenFilter extends TokenFilter {

    // The attribute to manupulate the token as a string
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    // The attribute to control the positions of the tokens
    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
    // This is the set for the common descriptors, this is the file for the descriptors
    private CommonDescriptorsSet descriptorsSet = new CommonDescriptorsSet("files/descriptors");
    // This will contain the tokens, because is more easy to control in a list
    public List<String> tokens = new ArrayList<String>();
    // Bolleans to help the operations
    public boolean consumed = false, removed = false, constructed = false,
            removeIt;
    // Auxiliar variables
    public int pos, start;
    public String text;

    /**
     * The default constructor for TokenFilter
     *
     * @param input the input TokenStream
     */
    public CommonDescriptorsTokenFilter(TokenStream input) {
        super(input);
    }

    @Override
    public boolean incrementToken() throws IOException {
        // If we not consumed all tokens, then cosume it!
        if (!consumed) {
            consumed = true;
            while (input.incrementToken()) {
                tokens.add(termAtt.toString());
            }
            // The start position
            pos = 0;
            return true;
        } else {
            // If we not remove the common descriptors, and the size of the
            // name it's greater than 2
            if (!removed && tokens.size() > 2) {
                // Remove the common descriptors
                while (pos < tokens.size()) {
                    if (descriptorsSet.contains(tokens.get(pos))) {
                        // In there we may remove it
                        removeIt = true;
                        int j = pos;
                        while (j < tokens.size()) {
                            // But if have some word after this that is not an
                            // common descriptor, we will not remove it
                            if (!descriptorsSet.contains(tokens.get(j))) {
                                removeIt = false;
                            }
                            j++;
                        }
                        // If we need to remove, so, remove
                        if (removeIt) {
                            tokens.remove(pos);
                        }
                    }
                    // If the size of the token is smaller than 1, remove!
                    if (pos < tokens.size() && tokens.get(pos).length() <= 1) {
                        tokens.remove(pos);
                    }
                    pos++;
                }
                removed = true;
                return true;
            }
        }
        // If we already pass through the remove operation, so let's construct
        // the TokenStream
        if (removed || tokens.size() <= 2) {
            // if we not contruct the text, so construct it, this is better to
            // create the TokenStream
            if (!constructed) {
                text = "";
                for (String token : tokens) {
                    text = text + token + " ";
                }
                constructed = true;
                text = text.trim();
                // Flush the input TokenStream to create the TokenStream without
                // the common descriptors
                termAtt.setEmpty();
                start = 0;
            }
            // Creates the new TokenStream
            if (start < text.length() && !text.isEmpty()) {
                int i;
                for (i = start; i < text.length(); i++) {
                    // If it's a withespace character, create the token
                    if (Character.isWhitespace(text.charAt(i))) {
                        offsetAtt.setOffset(start, i);
                        termAtt.copyBuffer(text.toCharArray(), start, i - start);
                        start = i + 1;
                        return true;
                    }
                }
                // If we research to the final of the string, create the last 
                // token
                if (i == text.length()) {
                    offsetAtt.setOffset(start, i);
                    termAtt.copyBuffer(text.toCharArray(), start, i - start);
                    start = i + 1;
                    return true;
                }
            }
        }
        // Flush the results because the reuse strategy of the Lucene
        this.flush();
        return false;
    }

    private void flush() {
        tokens = new ArrayList<String>();
        consumed = false;
        removed = false;
        constructed = false;
    }

}
