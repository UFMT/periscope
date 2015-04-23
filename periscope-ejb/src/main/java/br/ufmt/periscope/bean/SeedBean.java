package br.ufmt.periscope.bean;

import br.ufmt.periscope.indexer.LuceneIndexerResources;
import br.ufmt.periscope.indexer.resources.analysis.CommonDescriptor;
import br.ufmt.periscope.model.ApplicantType;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.bson.types.Code;

import br.ufmt.periscope.model.Country;
import br.ufmt.periscope.model.User;

import com.bigfatgun.fixjures.Fixjure;
import com.bigfatgun.fixjures.yaml.YamlSource;
import com.github.jmkgreen.morphia.Datastore;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Manifest;
import javax.enterprise.inject.Produces;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.RequestScoped;
import javax.inject.Named;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;

@ApplicationScoped
@Singleton
@Startup
/**
 * Isere dados iniciais nos documentos de Usuários, Países, Descritores comuns e
 * Natureza das Patentes
 */
public class SeedBean {

    private @Inject
    Datastore ds;
    private @Inject
    Logger log;
    private @Inject
    LuceneIndexerResources resources;
    private IndexWriter writer;
    public static String PERISCOPE_DIR;

    private String versionNumber = "";

    static {
        PERISCOPE_DIR = System.getenv("PERISCOPE_DIR");
        if (PERISCOPE_DIR == null) {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                PERISCOPE_DIR = "C:\\ProgramData\\Periscope";
            } else {
                PERISCOPE_DIR = "/opt/periscope";
            }
            File dir = new File(PERISCOPE_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }

    }

    @PostConstruct
    /**
     * Método executado quando é implantado no servidor
     */
    public void atStartup() {

        log.info("Inicializando seeder");
        initUsers();
        initCountries();
        initApplicantTypes();
        initCommonsDescriptors();
        insertAlgorithFromFile("lcs", "js/longestCommonSubstring.js");
        insertAlgorithFromFile("levenshtein", "js/levenshtein.js");
        insertAlgorithFromFile("LiquidMetal", "js/liquidmetal.js");

        try {
            Enumeration<URL> resources = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                if (url.getFile().contains("periscope-ejb")) {
                    try {
                        Manifest manifest = new Manifest(url.openStream());
                        // check that this is your manifest and do what you need or get the next one
                        versionNumber = "";
                        java.util.jar.Attributes attributes = manifest.getMainAttributes();
                        if (attributes != null) {
                            java.util.Iterator it = attributes.keySet().iterator();
                            while (it.hasNext()) {
                                java.util.jar.Attributes.Name key = (java.util.jar.Attributes.Name) it.next();
                                String keyword = key.toString();
                                if (keyword.equals("Implementation-Build")) {
                                    versionNumber = (String) attributes.get(key);
                                    break;
                                }
                            }
                        }
                    } catch (IOException E) {
                        // handle
                    }
                }
            }
        } catch (IOException E) {
            // handle
            E.printStackTrace();
        }
    }

    @Produces
    @Named(value = "versionNumber")
    public String getVersion() {
        return versionNumber;
    }

    /**
     * Insere algoritmos javascript no MongoDB
     *
     * @param name Nome da função para ser chamada no Mongo
     * @param path Caminho para o arquivo do algoritmo da função
     */
    private void insertAlgorithFromFile(String name, String path) {

        DB db = ds.getDB();

        DBCollection functionsCollection = db.getCollectionFromString("system.js");
        InputStream is = SeedBean.class.getClassLoader().getResourceAsStream(path);
        String function = new Scanner(is, "UTF-8").useDelimiter("\\A").next();

        if (function == null) {
            log.log(Level.SEVERE, "Erro ao ler o arquivo com a função " + name);
        }
        Code code = new Code(function);
        BasicDBObject newFunction = new BasicDBObject();
        newFunction.put("_id", name);
        newFunction.put("value", code);

        functionsCollection.save(newFunction);

    }

    /**
     * Inicia as naturezas das patentes a partir do arquivo yaml correspondente
     */
    private void initApplicantTypes() {
        if (ds.getCount(ApplicantType.class) == 0l) {
            log.info("Nenhuma Natureza encontrada.");
            List<ApplicantType> applicantTypes = Fixjure
                    .listOf(ApplicantType.class)
                    .from(YamlSource
                            .newYamlResource("applicantType-inicial.yaml"))
                    .create();
            Iterator<ApplicantType> it = applicantTypes.iterator();
            while (it.hasNext()) {
                ds.save(it.next());
            }
            log.info("Cadastrado " + applicantTypes.size() + " Naturezas.");
        }

    }

    /**
     * Inicia os países a partir do arquivo yaml correspondente
     */
    private void initCountries() {
        if (ds.getCount(Country.class) == 0l) {
            log.info("Nenhum país encontrado.");
            List<Country> countries = Fixjure
                    .listOf(Country.class)
                    .from(YamlSource
                            .newYamlResource("country-inicial-data.yaml"))
                    .create();
            Iterator<Country> it = countries.iterator();
            while (it.hasNext()) {
                ds.save(it.next());
            }
            log.info("Cadastrado " + countries.size() + " países.");
        }
    }

    /**
     * Inicia os usuários iniciais a partir do arquivo yaml correspondente
     */
    private void initUsers() {
        if (ds.getCount(User.class) == 0l) {
            log.info("Nenhum usuário encontrado.");
            List<User> users = Fixjure.listOf(User.class)
                    .from(YamlSource.newYamlResource("user-inicial.yaml"))
                    .create();
            Iterator<User> it = users.iterator();
            while (it.hasNext()) {
                ds.save(it.next());
            }
            log.info("Cadastrado " + users.size() + " usuários.");
        }
    }

    /**
     * Inicia os descritores comuns para o processo de harmonização a partir do
     * arquivo yaml correspondente
     */
    private void initCommonsDescriptors() {
        if (ds.getCount(CommonDescriptor.class) == 0l) {
            writer = resources.getIndexWriter();
            log.info("Nenhum descritor comum encontrado.");
            List<CommonDescriptor> descriptors = Fixjure.listOf(CommonDescriptor.class)
                    .from(YamlSource.newYamlResource("descriptors.yaml")).create();
            Iterator<CommonDescriptor> it = descriptors.iterator();
            while (it.hasNext()) {
                CommonDescriptor desc = it.next();
                ds.save(desc);
                Document doc = new Document();
                doc.add(new TextField("id", desc.getWord(), Field.Store.YES));
                try {
                    writer.deleteDocuments(new Term("id", doc.get("id")));
                    writer.addDocument(doc);
                } catch (IOException ex) {
                    Logger.getLogger(SeedBean.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            log.info("Cadastrado " + descriptors.size() + " descritores comuns.");
            resources.closeWriter(writer);
        }
    }
}
