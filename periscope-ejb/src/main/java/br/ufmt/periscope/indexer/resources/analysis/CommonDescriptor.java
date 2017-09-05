package br.ufmt.periscope.indexer.resources.analysis;

import com.github.jmkgreen.morphia.annotations.Entity;
import com.github.jmkgreen.morphia.annotations.Id;
import java.io.Serializable;
import java.util.List;

@Entity
/**
 * Classe dos descritores comuns. Utilizado para o processo de harmonização. Os
 * descritores comuns são palavras que aparecem em nome de empresas e servem
 * para descrever o tipo jurídico ou ramo de atualçai da empresa. Ex: SA, LTD,
 * CO, CORP....
 */
public class CommonDescriptor implements Serializable {

    @Id
    private String word;
    private List<List<String>> followedBy;
    private List<List<String>> precededBy;
    private long count;
    private List<String> variations;
    private List<List<String>> expansion;
    private boolean onlyFollowedBy;
    private boolean onlyPrecededBy;

    /**
     * Getter do valor string do descritor
     *
     * @return String O valor string do descritor
     */
    public String getWord() {
        return word;
    }

    /**
     * Setter do valor string do descritor comnum
     *
     * @param word String com o valor do descritor comum
     */
    public void setWord(String word) {
        this.word = word;
    }

    /**
     * Getter da(s) lista(s) de palavra(s) que sucedem o descritor comum.
     *
     * @return List<List<String>> a(s) lista(s) de palavra(s) que aparecem
     * sucedendo o descritor comum
     */
    public List<List<String>> getFollowedBy() {
        return followedBy;
    }

    /**
     * Setter da(s) lista(s) de palavra(s) que sucedem o descritor comum
     *
     * @param followedBy a(s) lista(s) de palavra(s)
     */
    public void setFollowedBy(List<List<String>> followedBy) {
        this.followedBy = followedBy;
    }

    /**
     * Getter da(s) lista(s) de palavra(s) que precedem o descritor comum.
     *
     * @return List<List<String>> a(s) lista(s) de palavra(s) que aparecem
     * precedendo o descritor comum
     */
    public List<List<String>> getPrecededBy() {
        return precededBy;
    }

    /**
     * Setter da(s) lista(s) de palavra(s) que precedem o descritor comum
     *
     * @param precededBy a(s) lista(s) de palavra(s)
     */
    public void setPrecededBy(List<List<String>> precededBy) {
        this.precededBy = precededBy;
    }

    /**
     * Getter do número de vezes que o valor string do descritor comum apareceu
     * na base de dados.
     *
     * @return long A quantidade de vezes
     */
    public long getCount() {
        return count;
    }

    /**
     * Setter do número de vezes que o valor string do descritor comum apareceu
     * na base de dados
     *
     * @param count a quantidade de vezes
     */
    public void setCount(long count) {
        this.count = count;
    }

    /**
     * Getter da lista de variações existentes do descritor comum. O valor
     * escrito em outras linguas ou suas abreviações.
     *
     * @return List<String> as variações do valor string do descritor comum
     */
    public List<String> getVariations() {
        return variations;
    }

    /**
     * Setter da lista de variações existentes do valor string do descritor. O
     * valor escritor em outras linguas ou suas abreviações. comum
     *
     * @param variations As variações
     */
    public void setVariations(List<String> variations) {
        this.variations = variations;
    }

    /**
     * Getter da(s) expansão(ões) existente(s) do valor string do descritor
     * comum, isto se o descritor for uma abreviação.
     *
     * @return A(s) lista(s) da(s) expansão(ões) existentes do descritor comum
     */
    public List<List<String>> getExpansion() {
        return expansion;
    }

    /**
     * Setter da(s) expansão(ões) existente(s) do valor string do descritor
     * comum, isto se o descritor for uma abreviação.
     *
     * @param expansion
     */
    public void setExpansion(List<List<String>> expansion) {
        this.expansion = expansion;
    }

    /**
     * Getter do valor que indica se o descritor comum pode ser removido apenas
     * se o mesmo se encontrar sucedido por alguma palavras ou lista de palavras
     * existente no atributo followedBy
     *
     * @return boolean true se é removido apenas se for sucedido pelas palavras,
     * false caso contrário
     */
    public boolean isOnlyFollowedBy() {
        return onlyFollowedBy;
    }

    /**
     * Setter do valor que indica se o descritor comum pode ser removido apenas
     * se o mesmo se encontrar sucedido por alguma palavras ou lista de palavras
     * existente no atributo followedBy
     *
     * @param onlyFollowedBy
     */
    public void setOnlyFollowedBy(boolean onlyFollowedBy) {
        this.onlyFollowedBy = onlyFollowedBy;
    }

    /**
     * Getter do valor que indica se o descritor comum pode ser removido apenas
     * se o mesmo se encontrar precedido por alguma palavras ou lista de
     * palavras existente no atributo precededBy
     *
     * @return boolean true se é removido apenas se for precidido pelas
     * palavras, false caso contrário
     */
    public boolean isOnlyPrecededBy() {
        return onlyPrecededBy;
    }

    /**
     * Setter do valor que indica se o descritor comum pode ser removido apenas
     * se o mesmo se encontrar precedido por alguma palavras ou lista de palavras
     * existente no atributo precidedBy
     *
     * @param onlyPrecededBy
     */
    public void setOnlyPrecededBy(boolean onlyPrecededBy) {
        this.onlyPrecededBy = onlyPrecededBy;
    }

}
