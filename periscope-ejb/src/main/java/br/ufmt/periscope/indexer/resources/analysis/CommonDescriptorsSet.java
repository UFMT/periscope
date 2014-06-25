package br.ufmt.periscope.indexer.resources.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class will contains the commons company descriptors Now, it's just use a
 * ArrayList, but this may change in the future
 *
 * @author mattyws
 */
public class CommonDescriptorsSet {

    private List<String> commonDescriptors = new ArrayList<String>();
    private BufferedReader reader;

    /**
     * The constructor
     *
     * @param caminho path for the file containing the commons descriptors
     */
    public CommonDescriptorsSet(String caminho) {
        try {
            File file = new File(caminho);
            reader = new BufferedReader(new FileReader(caminho));
            this.readFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Read the file passed by parameter in the constructor
     *
     * @throws IOException
     */
    private void readFile() throws IOException {
        String linha;
        while (reader.ready()) {
            linha = reader.readLine();
            commonDescriptors.add(linha);
        }
        reader.close();
    }

    /**
     * See if a string exists in the list
     *
     * @param descriptor the text that will be searched
     * @return true if exists in list, false otherwise
     */
    public boolean contains(String descriptor) {
        return commonDescriptors.contains(descriptor);
    }

}
