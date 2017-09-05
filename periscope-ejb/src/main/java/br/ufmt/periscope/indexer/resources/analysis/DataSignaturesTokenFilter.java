package br.ufmt.periscope.indexer.resources.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

/**
 *Class to generate Token-Sensitive Signatures for finding candidates faster
 *
 * @author horgun
 *
 */
public class DataSignaturesTokenFilter extends TokenFilter {
    
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
    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
    private final float editSimilarityThreshold = 0.75f;
    private final float tokenThreshold = 0.7f;
    private ArrayList<String> substrings = new ArrayList();
    private ArrayList<Signature> signatures = new ArrayList();
    private int tokenNumber = 0;
    private boolean consumed = false;
    private String tokenSet = "";
    /**
     * The default constructor of TokenFilter
     * @param input the input TokenStream
     */
    protected DataSignaturesTokenFilter(TokenStream input) {
        super(input);
    }

    @Override
    public final boolean incrementToken() throws IOException {
        if (!consumed){
            while (substrings.isEmpty() && input.incrementToken()){
                tokenSet += termAtt.toString() + " ";
                consumed = true;
                tokenNumber++;
                int startLength = (int) Math.ceil(editSimilarityThreshold * termAtt.toString().length());
                int endLength = (int) Math.ceil(termAtt.toString().length() / editSimilarityThreshold);

                //Generate substrings
                for (int i = startLength; i <= endLength; i++){

                    int maxEditDistThresh = (int) Math.floor(((1 - editSimilarityThreshold)/editSimilarityThreshold) * i);
                    int partitions = (int) Math.ceil((maxEditDistThresh + 1)/2.0);
                    int editDistThresh = (int) Math.floor((1 - editSimilarityThreshold) * Integer.max(termAtt.toString().length(), i));
                    int l = (int) Math.floor(i/partitions);//tamanho de t' / d

                    //Case 1 - the first partition
                    for (int j = l-2; j <= l; j++){// [v-1, v+1]

                        int minEditDist = Math.abs((j) - (l-1)) + Math.abs((termAtt.toString().length()-1 -j) - (i-1 -l));
                        if (j+1 < termAtt.toString().length() && minEditDist <= editDistThresh && !substrings.contains(termAtt.toString().substring(0, j+1)))
                            substrings.add(termAtt.toString().substring(0, j+1));
                    }

                    //Case 2 - the last partition
                    int lastPartitionSize = i - ((partitions - 1) * l);
                    for (int j = termAtt.toString().length() - lastPartitionSize - 1; j <= termAtt.toString().length() - lastPartitionSize +1; j++){//[v-1, v+1]

                        int minEditDist = Math.abs((termAtt.toString().length()-1 - j) - ((i-1)- ((partitions - 1) * l))) + Math.abs(j-((partitions - 1) * l));
                        if (j >= 0 && minEditDist <= editDistThresh && !substrings.contains(termAtt.toString().substring(j, termAtt.toString().length())))
                            substrings.add(termAtt.toString().substring(j, termAtt.toString().length()));
                    }

                    //Case 3 - the middle partitions
                    for (int j = l; j < ((partitions - 1) * l); j += l){

                        for (int k = j - editDistThresh; k <= j + editDistThresh; k++){
                            if (k < 0)
                                continue;
                            
                            //for minimal edit distance pruning
                            
                            int minEditDist1 = Math.abs(((k+l-2)-k) - ((j+l-1)-j)) + Math.abs(k-j) + Math.abs((termAtt.toString().length() -(k+l-2)) - (i -(j+l-1)));
                            int minEditDist2 = Math.abs(((k+l-1)-k) - ((j+l-1)-j)) + Math.abs(k-j) + Math.abs((termAtt.toString().length() -(k+l-1)) - (i -(j+l-1)));
                            int minEditDist3 = Math.abs(((k+l)-k) - ((j+l-1)-j)) + Math.abs(k-j) + Math.abs((termAtt.toString().length() -(k+l)) - (i -(j+l-1)));

                            if (k + l - 2 < termAtt.toString().length() && minEditDist1 <= editDistThresh && k <= (j+editDistThresh-(2*j/l)) && k >= (j-(editDistThresh - (2*j/l))) && !substrings.contains(termAtt.toString().substring(k, k + l - 1)))
                                substrings.add(termAtt.toString().substring(k, k + l - 1));
                            if (k + l - 1 < termAtt.toString().length() && minEditDist2 <= editDistThresh && k <= (j+editDistThresh-(2*j/l)) && k >= (j-(editDistThresh - (2*j/l))) && !substrings.contains(termAtt.toString().substring(k, k + l)))
                                substrings.add(termAtt.toString().substring(k, k + l));
                            if (k + l < termAtt.toString().length() && minEditDist3 <= editDistThresh && k <= (j+editDistThresh-(2*j/l)) && k >= (j-(editDistThresh - (2*j/l))) && !substrings.contains(termAtt.toString().substring(k, k + l +1)))
                                substrings.add(termAtt.toString().substring(k, k + l +1));
                        }
                    }
                }

                //Generate 0 and 1 deletions signatures from substrings
                for (String substring : substrings) {
                    if (!signatures.contains(new Signature(substring, tokenNumber)))
                        signatures.add(new Signature(substring, tokenNumber));//0 deletion
                    for (int i = 0; i < substring.length(); i++){//1 deletion
                        if (i == 0 && substring.length() >= 2 && !signatures.contains(new Signature(substring.substring(1), tokenNumber)))
                            signatures.add(new Signature(substring.substring(1), tokenNumber));
                        else if (i == 1 && substring.length() == 2 && !signatures.contains(new Signature(substring.substring(0, 1), tokenNumber)))
                            signatures.add(new Signature(substring.substring(0, 1), tokenNumber));
                        else if (!signatures.contains(new Signature(substring.substring(0, i) + substring.substring(i+1), tokenNumber)))
                            signatures.add(new Signature(substring.substring(0, i) + substring.substring(i+1), tokenNumber));
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
            float c = tokenThreshold * tokenSetTokens.length;
            
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
            termAtt.setEmpty();
            String tokenSetTokens[] = tokenSet.split(" ");
            int start = tokenSet.indexOf(tokenSetTokens[s.getTid()-1]);
            int end = start + tokenSetTokens[s.getTid()-1].length()-1;
//            System.out.println("Start: " + start + "; end: " + end + ";");
            offsetAtt.setOffset(start, end);
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
