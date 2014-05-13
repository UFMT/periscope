package br.ufmt.periscope.indexer;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

// CondenseFilter code was not analyzed yet, i take from a stack overflow answer
//Link : http://stackoverflow.com/questions/7384016/whats-wrong-with-this-lucene-tokenfilter
public class CondenseFilter extends TokenFilter {

	private final StringBuilder sb = new StringBuilder();
	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
	private boolean consumed; // true if we already consumed

	protected CondenseFilter(TokenStream input) {
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
		while (input.incrementToken()) {
			if (!found) {
				startOffset = offsetAtt.startOffset();
				found = true;
			}
			sb.append(termAtt);
			endOffset = offsetAtt.endOffset();
		}

		if (found) {
			assert sb.length() > 0; // always: because we append separator
			sb.setLength(sb.length());
			clearAttributes();
			termAtt.setEmpty().append(sb);
			offsetAtt.setOffset(startOffset, endOffset);
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
