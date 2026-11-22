package br.ufmt.periscope.bean;

import br.ufmt.periscope.indexer.LuceneIndexerResources;
import br.ufmt.periscope.indexer.resources.analysis.CommonDescriptor;
import br.ufmt.periscope.model.ApplicantType;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;

import br.ufmt.periscope.model.Country;
import br.ufmt.periscope.model.User;
import br.ufmt.periscope.security.PasswordHasher;
import br.ufmt.periscope.util.YamlLoader;

import dev.morphia.Datastore;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Manifest;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
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
        // Lucene 6 indexes are not readable by Lucene 9. After upgrading, clear
        // existing Lucene files under PERISCOPE_DIR before the first deploy so a
        // fresh index can be created (see LuceneIndexerResources).
        PERISCOPE_DIR = System.getenv("PERISCOPE_DIR");
        if (PERISCOPE_DIR == null || PERISCOPE_DIR.isBlank()) {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                PERISCOPE_DIR = "C:\\ProgramData\\Periscope";
            } else {
                PERISCOPE_DIR = "/opt/periscope";
            }
        }
        File dir = new File(PERISCOPE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
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
        // system.js / db.eval removed in modern MongoDB; algorithms run in Java (RuleRepository)

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
     * Inicia as naturezas das patentes a partir do arquivo yaml correspondente
     */
    private void initApplicantTypes() {
        if (ds.find(ApplicantType.class).count() == 0L) {
            log.info("Nenhuma Natureza encontrada.");
            List<ApplicantType> applicantTypes = YamlLoader
                    .loadList("applicantType-inicial.yaml", ApplicantType.class);
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
        if (ds.find(Country.class).count() == 0L) {
            log.info("Nenhum país encontrado.");
            List<Country> countries = YamlLoader
                    .loadList("country-inicial-data.yaml", Country.class);
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
        if (ds.find(User.class).count() == 0L) {
            log.info("Nenhum usuário encontrado.");
            List<User> users = YamlLoader.loadList("user-inicial.yaml", User.class);
            applyAdminEnvOverrides(users, System::getenv);
            Iterator<User> it = users.iterator();
            while (it.hasNext()) {
                User user = it.next();
                if (user.getPassword() != null && PasswordHasher.needsRehash(user.getPassword())) {
                    user.setPassword(PasswordHasher.hash(user.getPassword()));
                }
                ds.save(user);
            }
            log.info("Cadastrado " + users.size() + " usuários.");
        }
    }

    public static void applyAdminEnvOverrides(List<User> users, java.util.function.Function<String, String> env) {
        if (users == null || users.isEmpty() || env == null) {
            return;
        }
        User admin = users.get(0);
        String username = env.apply("PERISCOPE_ADMIN_USERNAME");
        if (username != null && !username.isBlank()) {
            admin.setUsername(username.trim());
        }
        String password = env.apply("PERISCOPE_ADMIN_PASSWORD");
        if (password != null && !password.isBlank()) {
            admin.setPassword(password);
        }
        String email = env.apply("PERISCOPE_ADMIN_EMAIL");
        if (email != null && !email.isBlank()) {
            admin.setEmail(email.trim());
        }
        String forceChange = env.apply("PERISCOPE_FORCE_ADMIN_PASSWORD_CHANGE");
        if ("true".equalsIgnoreCase(forceChange)) {
            admin.setMustChangePassword(true);
        }
    }

    /**
     * Inicia os descritores comuns para o processo de harmonização a partir do
     * arquivo yaml correspondente
     */
    private void initCommonsDescriptors() {
        if (ds.find(CommonDescriptor.class).count() == 0L) {
            log.info("Nenhum descritor comum encontrado.");
            writer = resources.getIndexWriter();
            if (writer == null) {
                log.log(Level.SEVERE,
                        "Lucene IndexWriter unavailable under {0}; seeding descriptors to Mongo only",
                        PERISCOPE_DIR);
            }
            List<CommonDescriptor> descriptors = YamlLoader
                    .loadList("descriptors.yaml", CommonDescriptor.class);
            Iterator<CommonDescriptor> it = descriptors.iterator();
            while (it.hasNext()) {
                CommonDescriptor desc = it.next();
                ds.save(desc);
                if (writer == null) {
                    continue;
                }
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
            if (writer != null) {
                resources.closeWriter(writer);
            }
        }
    }
}
