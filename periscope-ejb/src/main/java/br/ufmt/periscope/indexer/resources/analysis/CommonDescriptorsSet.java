package br.ufmt.periscope.indexer.resources.analysis;

import com.github.jmkgreen.morphia.Datastore;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * O conjunto de descritores comuns. Gerencia as buscas a base de dados de
 * descritores comuns.
 *
 */
@Named
public class CommonDescriptorsSet {

    private @Inject
    Datastore ds;

    /**
     * Construtor simples da classe
     */
    public CommonDescriptorsSet() {
    }

    /**
     * Verifica se a palavra é um descritor comum
     * @param descriptor a palavra a ser buscada
     * @return true se a palavra existe na base de dados, false caso contrário.
     */
    public boolean contains(String descriptor) {
        List<CommonDescriptor> descriptorSet = ds
                .find(CommonDescriptor.class)
                .field("_id").equal(descriptor)
                .asList();

        if (descriptorSet != null && !descriptorSet.isEmpty()) {
            return true;
        }
        return false;
    }

}
