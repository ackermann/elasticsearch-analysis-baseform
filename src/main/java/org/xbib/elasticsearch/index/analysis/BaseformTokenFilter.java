
package org.xbib.elasticsearch.index.analysis;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.util.AttributeSource;

import org.xbib.elasticsearch.analysis.baseform.Dictionary;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.util.LinkedList;

public class BaseformTokenFilter extends TokenFilter {

    private final LinkedList<Token> tokens;

    private final Dictionary dictionary;

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);

    private final PositionIncrementAttribute posIncAtt = addAttribute(PositionIncrementAttribute.class);

    private AttributeSource.State current;

    protected BaseformTokenFilter(TokenStream input, Dictionary dictionary) {
        super(input);
        this.tokens = new LinkedList<Token>();
        this.dictionary = dictionary;
    }

    @Override
    public final boolean incrementToken() throws IOException {
        if (!tokens.isEmpty()) {
            assert current != null;
            Token token = tokens.removeFirst();
            restoreState(current);
            termAtt.setEmpty().append(token);
            offsetAtt.setOffset(token.startOffset(), token.endOffset());
            posIncAtt.setPositionIncrement(0);
            return true;
        }
        if (input.incrementToken()) {
            injectBaseform();
            if (!tokens.isEmpty()) {
                current = captureState();
            }
            return true;
        } else {
            return false;
        }
    }

    protected void injectBaseform() throws CharacterCodingException {
        int start = offsetAtt.startOffset();
        int end = offsetAtt.endOffset();
        CharSequence term = new String(termAtt.buffer(), 0, termAtt.length());
        CharSequence s = dictionary.lookup(term);
        if (s != null) {
            tokens.add(new Token(s.toString(), start, end));
        }
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        tokens.clear();
        current = null;
    }

    public void close() throws IOException {
        super.close();
        tokens.clear();
        current = null;
    }

}
