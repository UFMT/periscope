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
    private int startOffset;

    /**
     * The default constructor of TokenFilter
     * @param input the input TokenStream
     */
    protected CondenseTokenFilter(TokenStream input) {
        super(input);
    }

    @Override
    public final boolean incrementToken() throws IOException {
        if (!consumed){
            consumed = true;
            
            boolean found = false; // true if we actually consumed any tokens
            // Lets consume all the tokens
            while (input.incrementToken()) {
                if (!found) {
                    found = true;
                }
                sb.append(termAtt);
                // The '_' is just to create the acronym after
                sb.append("_");
            }
            
            // If we found any token
            if (found) {
                // Clear the attributes to have no error
                clearAttributes();
                String name = sb.toString().replaceAll("[^A-Za-z0-9_]", "");
//                String acronimo = "";
//                String name2 = "";

                if (name.length() > 0) {
                    name = name.trim();                
                    String[] words = name.split("_");
                    name = "";
                    for(String word : words) {
                        if (word.trim().length() > 0) {
                            name += word + '_';
//                            name2 += word + ' ';
                        }
                        //Se tamanho do token for maior que 1, pegar a primeira letra.
//                        if (word.trim().length() > 1)
//                            acronimo += word.charAt(0);
                    }
                    if(name.length() > 0){
                        name = name.substring(0, name.length()-1);//Removes the _ at the end
//                        name2 = name2.substring(0, name2.length()-1);
                    }
                    else
                        return false;
                    sb.delete(0, sb.length());
                    sb.append(name);
                    
//                    if (acronimo.length() > 1)
//                        sb.append(" ").append(acronimo);
//                    sb.append(" ").append(name2);
                    
                    termAtt.setEmpty();
                    startOffset = 0;
                }
            } else {
                return false;
            }
        }
        if (startOffset < sb.length()){
            for (int i = startOffset; i <  sb.length(); i++){
                if (Character.isWhitespace(sb.charAt(i))){
                    offsetAtt.setOffset(startOffset, i);
                    termAtt.copyBuffer(sb.toString().toCharArray(), startOffset, i - startOffset);
                    startOffset = i + 1;
                    return true;
                }
                if (i + 1 == sb.length()){
                    offsetAtt.setOffset(startOffset, i+1);
                    termAtt.copyBuffer(sb.toString().toCharArray(), startOffset, i+1 - startOffset);
                    startOffset = i + 1;
                    return true;
                }
            }
        }
        
        return false;
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        sb.setLength(0);
        startOffset = 0;
        consumed = false;
    }

}
