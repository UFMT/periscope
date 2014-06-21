package br.ufmt.periscope.indexer.resources.analysis;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

/**
 * Breaks tokens in withespace characters
 *
 * @author mattyws
 */
public final class WhitespaceTokenFilter extends TokenFilter {

    private char[] curTermBuffer;

    private int curTermLength;

    private int currentOffset;

    private int baseOffset;

    private CharTermAttribute termAtt;

    private OffsetAttribute offsetAtt;

    /**
     * The constructor
     *
     * @param input the input TokenStream
     */
    protected WhitespaceTokenFilter(TokenStream input) {
        super(input);
        this.termAtt = addAttribute(CharTermAttribute.class);
        this.offsetAtt = addAttribute(OffsetAttribute.class);
    }

    @Override
    public final boolean incrementToken() throws IOException {
        while (true) {
            // If the termBuffer is not already set, so, we will create it
            // This buffer is the one token
            if (curTermBuffer == null) {
                // If we do not have a token anymore
                if (!input.incrementToken()) {
                    return false;
                } else {
                    // Clone the buffer for the token
                    curTermBuffer = (char[]) termAtt.buffer().clone();
                    // Take it's length
                    curTermLength = termAtt.length();
                    // This is the offset for the token
                    currentOffset = 0;
                    baseOffset = offsetAtt.startOffset();
                }
            }
            // If the currentOffset  is less than the 
            // token length
            if (currentOffset < curTermLength) {
                // Loop through the characters
                for (int i = currentOffset; i < curTermLength - 1; i++) {
                    // If it's a withspace, let's break into tokens
                    if (Character.isWhitespace(curTermBuffer[i])) {
                        // This is the start of the token
                        int start = currentOffset;
                        // And where we found the withespace will be the end
                        int end = i;
                        // Set the offset for this token, copy the buffer
                        // and return true, this return will pass to the part
                        // that left to be processed in this token
                        offsetAtt.setOffset(baseOffset + start, baseOffset
                                + end);
                        termAtt.copyBuffer(curTermBuffer, start, end - start);
                        currentOffset = i + 1;
                        return true;
                    }
                }
                // If we loop through all the token and don't found a whitespace
                // character, we will create the token with this
                if (currentOffset < curTermLength) {
                    int start = currentOffset;
                    int end = curTermLength;
                    offsetAtt.setOffset(baseOffset + start, baseOffset + end);
                    termAtt.copyBuffer(curTermBuffer, start, end - start);
                    currentOffset = curTermLength;
                    return true;
                }
            }
            // Clear the buffer to pass to another token
            curTermBuffer = null;
        }
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        curTermBuffer = null;
    }
}
