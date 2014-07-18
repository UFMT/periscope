/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package br.ufmt.periscope.indexer.resources.analysis;

import com.github.jmkgreen.morphia.annotations.Entity;
import com.github.jmkgreen.morphia.annotations.Id;
import com.github.jmkgreen.morphia.annotations.Reference;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author mattyws
 */
@Entity
public class CommonDescriptor implements Serializable{
    
    @Id
    private String word;
    private List<List<String>> followedBy;
    private List<List<String>> precededBy;
    private long count;
    private List<String> variations;
    private List<List<String>> expansion;
    private boolean onlyFollowedBy;
    private boolean onlyPrecededBy;

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public List<List<String>> getFollowedBy() {
        return followedBy;
    }

    public void setFollowedBy(List<List<String>> followedBy) {
        this.followedBy = followedBy;
    }

    public List<List<String>> getPrecededBy() {
        return precededBy;
    }

    public void setPrecededBy(List<List<String>> precededBy) {
        this.precededBy = precededBy;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public List<String> getVariations() {
        return variations;
    }

    public void setVariations(List<String> variations) {
        this.variations = variations;
    }

    public List<List<String>> getExpansion() {
        return expansion;
    }

    public void setExpansion(List<List<String>> expansion) {
        this.expansion = expansion;
    }

    public boolean isOnlyFollowedBy() {
        return onlyFollowedBy;
    }

    public void setOnlyFollowedBy(boolean onlyFollowedBy) {
        this.onlyFollowedBy = onlyFollowedBy;
    }

    public boolean isOnlyPrecededBy() {
        return onlyPrecededBy;
    }

    public void setOnlyPrecededBy(boolean onlyPrecededBy) {
        this.onlyPrecededBy = onlyPrecededBy;
    }

    
}
