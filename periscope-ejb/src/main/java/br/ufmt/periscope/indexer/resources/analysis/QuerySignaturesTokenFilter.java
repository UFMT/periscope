package br.ufmt.periscope.indexer.resources.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 *Class to generate Token-Sensitive Signatures, the query signature set
 *
 * @author horgun
 *
 */
public class QuerySignaturesTokenFilter extends TokenFilter {
    
    public class Signature {
        private String value;
        private int tid;
        
        public Signature(String value, int tid){
            this.value = value;
            this.tid = tid;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public int getTid() {
            return tid;
        }

        public void setTid(int tid) {
            this.tid = tid;
        }

        @Override
        public String toString() {
            return value + tid; //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean equals(Object obj) {
            Signature s = (Signature) obj;
            return s.getValue().equals(this.getValue()) && s.getTid() == this.getTid();
        }
        
        
    }

    private final StringBuilder sb = new StringBuilder();
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final float editSimilarityThreshold = 0.75f;
    private ArrayList<String> substrings = new ArrayList();
    private ArrayList<Signature> signatures = new ArrayList();
    private int tokenNumber = 0;
    private boolean consumed = false;
    private String tokenSet = "";
    /**
     * The default constructor of TokenFilter
     * @param input the input TokenStream
     */
    protected QuerySignaturesTokenFilter(TokenStream input) {
        super(input);
    }

    @Override
    public final boolean incrementToken() throws IOException {
        if (!consumed){
            while (substrings.isEmpty() && input.incrementToken()){
                tokenSet += termAtt.toString() + " ";
                consumed = true;
                tokenNumber++;

                int maxEditDistThresh = (int) Math.floor(((1 - editSimilarityThreshold)/editSimilarityThreshold) * termAtt.length());
                int partitions = (int) Math.ceil((maxEditDistThresh + 1)/2.0);
                int l = (int) Math.floor(termAtt.length()/partitions);//tamanho de t' / d
                
                for (int i = 0; i < (partitions-1)*l; i+= l){
                    substrings.add(termAtt.toString().substring(i, i+l));
                }
                substrings.add(termAtt.toString().substring((partitions-1)*l, termAtt.length()));
                //Generate 0 and 1 deletions signatures from substrings
                for (String substring : substrings) {
                    
                    if (!signatures.contains(new Signature(substring, tokenNumber)))
                        signatures.add(new Signature(substring, tokenNumber));//0 deletion
                    for (int i = 0; i < substring.length(); i++){//1 deletion
                        if (i == 0 && substring.length() >= 2 && !signatures.contains(new Signature(substring.substring(1), tokenNumber))){
                            signatures.add(new Signature(substring.substring(1), tokenNumber));
                        }
                        else if (i == 1 && substring.length() == 2 && !signatures.contains(new Signature(substring.substring(0, 1), tokenNumber))){
                            signatures.add(new Signature(substring.substring(0, 1), tokenNumber));
                        }
                        else if (i >= 3 && !signatures.contains(new Signature(substring.substring(0, i) + substring.substring(i+1), tokenNumber))){
                            signatures.add(new Signature(substring.substring(0, i) + substring.substring(i+1), tokenNumber));
                        }
                    }
                }


                substrings.clear();
            }
            
            tokenSet = tokenSet.trim();
            
            //sort
            signatures.sort(new Comparator<Signature>() {//for sorted insertion
                @Override
                public int compare(Signature o1, Signature o2) {
                    if (o1.getValue().equals(o2.getValue())){
                        return o1.getTid() < o2.getTid() ? 1 : (o1.getTid() > o2.getTid() ? -1 : 0);//I've inverted the sorting
                    }
                    else{
                        return -1 * o1.getValue().compareTo(o2.getValue());// * (-1) to invert sorting
                    }
                }
            });
            
            //Token-Sensitive Signature Algorithm
            String tokenSetTokens[] = tokenSet.split(" ");
            float c = editSimilarityThreshold * tokenSetTokens.length;
            
            ArrayList<Integer> tids = new ArrayList();
            ArrayList<Signature> remover = new ArrayList();
            for (Signature s : signatures){
                if (!tids.contains(s.getTid())){
                    tids.add(s.getTid());
                    if (tids.size() >= c)
                        break;
                }
                remover.add(s);
            }
            
            signatures.removeAll(remover);
            
        }
        if (consumed && !signatures.isEmpty()){
            Signature s = signatures.remove(0);
//            System.out.println("QuerySig: " + s.getValue());
            termAtt.setEmpty();
            termAtt.append(s.getValue());
            return true;
        }
        
        substrings.clear();
        signatures.clear();
        consumed = false;
        tokenNumber = 0;
        tokenSet = "";
        return false;
    }
    
    @Override
    public void reset() throws IOException {
        super.reset();
        substrings.clear();
        signatures.clear();
        consumed = false;
        tokenNumber = 0;
        tokenSet = "";
    }

}
