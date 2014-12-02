/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufmt.periscope.indexer.resources.search;

import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author mattyws
 */
public class FuzzyTokenSimilarity {

    private DamerauLevenshteinAlgorithm dla = new DamerauLevenshteinAlgorithm(1, 1, 1, 1);
    float intersec = 0.0f;
    int t1Size = 1, t2Size = 1;
    float editThreshold, tokenThreshold, dice, cosine, jaccard;

    /**
     * 
     * @param tokenThreshold the threshold for token similarity search, must be between [0..1]
     * @param editThreshold  the threshold for demerau levenshtein distance, must be between [0..1]
     */
    public FuzzyTokenSimilarity(float tokenThreshold, float editThreshold) {
        this.editThreshold = editThreshold;
        this.tokenThreshold = tokenThreshold;
    }

    public float execute(String s1, String s2) {
        ArrayList<String> t1 = new ArrayList<String>(Arrays.asList(s1.split("_"))),
                t2 = new ArrayList<String>(Arrays.asList(s2.split("_")));
        t1Size = t1.size();
        t2Size = t2.size();
        intersec = 0.0f;
        for (int i = 0; i < t2.size(); i++) {
            float lower = -1;
            int lowest = 0;
            for (int j = 0; j < t1.size(); j++) {
                int distance = dla.execute(t1.get(j), t2.get(i));
                if (lower < 0 || lower > distance) {
                    lower = distance;
                    lowest = j;
                }
            }
            lower = (1 - (lower / Math.max(t1.get(lowest).length(), t2.get(i).length())));
            if (lower < editThreshold) {
                lower = 0;
            }
            intersec += lower;
        }
        if (intersec < (tokenThreshold / (2 - tokenThreshold)) * t1Size) {
            intersec = 0;
        }
        // Dice token similarity
        dice = (2 * intersec) / (t1Size + t2Size);
        // Cossine token similarity
        cosine = (float) (intersec / Math.sqrt(t1Size * t2Size));
        // Jaccard token similarity
        jaccard = intersec / (t1Size + t2Size - intersec);
        return intersec;
    }

    public float fuzzyDice() {
        if(dice <= tokenThreshold)
            return 0;
        return dice;
    }

    public float fuzzyCosine() {
        if(cosine <= tokenThreshold) 
            return 0;
        return cosine;
    }

    public float fuzzyJaccard() {
        if(jaccard <= tokenThreshold)
            return 0;
        return jaccard;
    }

    public float getDice() {
        return dice;
    }    

    public float getCosine() {
        return cosine;
    }

    public float getJaccard() {
        return jaccard;
    }
}
