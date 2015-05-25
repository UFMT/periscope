package br.ufmt.periscope.util;

import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.model.ApplicantType;
import br.ufmt.periscope.model.Classification;
import br.ufmt.periscope.model.Country;
import br.ufmt.periscope.model.Files;
import br.ufmt.periscope.model.History;
import br.ufmt.periscope.model.Inventor;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Priority;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.Rule;
import br.ufmt.periscope.model.State;
import br.ufmt.periscope.model.User;
import com.mongodb.gridfs.GridFS;
import java.util.ArrayList;
import java.util.List;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * - @Named<BR/>
 * Classe responsável por criar as instâncias de todos os objetos do modelo do projeto via CDI
 */
@Named
public class ResourcesLazy {

    private @Inject
    Instance<Applicant> applicantProvider;
    private @Inject
    Instance<ApplicantType> applicantTypeProvider;
    private @Inject
    Instance<Classification> classificationProvider;
    private @Inject
    Instance<Country> countryProvider;
    private @Inject
    Instance<Files> filesProvider;
    private @Inject
    Instance<History> historyProvider;
    private @Inject
    Instance<Inventor> inventorProvider;
    private @Inject
    Instance<Patent> patentProvider;
    private @Inject
    Instance<Priority> priorityProvider;
    private @Inject
    Instance<Project> projectProvider;
    private @Inject
    Instance<Rule> ruleProvider;
    private @Inject
    Instance<State> stateProvider;
    private @Inject
    Instance<User> userProvider;
    private @Inject
    Instance<GridFS> fsProvider;
    
    /**
     * Método utilizado para criar uma nova instância de depositante utilizando CDI
     * @return uma instância de um depositante
     */
    public Applicant getApplicant() {
        return applicantProvider.get();
    }

    /**
     *
     * Método utilizado para criar uma nova instância da natureza de depositante utilizando CDI
     * @return uma instância de uma natureza de depositante
     */
    public ApplicantType getApplicantType() {
        return applicantTypeProvider.get();
    }

    /**
     *
     * Método utilizado para criar uma nova instância de classificação utilizando CDI
     * @return uma instância de uma classificação
     */
    public Classification getClassification() {
        return classificationProvider.get();
    }

    /**
     * Método utilizado para criar uma nova instância de país utilizando CDI
     * @return uma instância de um país
     */
    public Country getCountry() {
        return countryProvider.get();
    }

    /**
     * Método utilizado para criar uma nova instância de arquivo utilizando CDI
     * @return uma instância de um arquivo
     */
    public Files getFiles() {
        return filesProvider.get();
    }

    /**
     * Método utilizado para criar uma nova instância de histórico utilizando CDI
     * @return uma instância de um histórico
     */
    public History getHistory() {
        return historyProvider.get();
    }

    /**
     * Método utilizado para criar uma nova instância de inventor utilizando CDI
     * @return uma instância de um inventor
     */
    public Inventor getInventor() {
        return inventorProvider.get();
    }

    /**
     * Método utilizado para criar uma nova instância de patente utilizando CDI
     * @return uma instância de uma patente
     */
    public Patent getPatent() {
        return patentProvider.get();
    }

    /**
     * Método utilizado para criar uma nova instância de prioridade utilizando CDI
     * @return uma instância de uma prioridade
     */
    public Priority getPriority(){
        return priorityProvider.get();
    }
    
    /**
     * Método utilizado para criar uma nova instância de projeto utilizando CDI
     * @return uma instância de um projeto
     */
    public Project getProject(){
        return projectProvider.get();
    }
                    
    /**
     * Método utilizado para criar uma nova instância de regra utilizando CDI
     * @return uma instância de uma regra
     */
    public Rule getRule(){
        return ruleProvider.get();
    }
    
    /**
     * Método utilizado para criar uma nova instância de estado utilizando CDI
     * @return uma instância de um estado
     */
    public State getState(){
        return stateProvider.get();
    }
    
    /**
     * Método utilizado para criar uma nova instância de usuário utilizando CDI
     * @return uma instância de um usuário
     */
    public User getUser(){
        return userProvider.get();
    }
    
    /**
     * Método utilizado para criar uma nova instância de FileSystem de conexão com o banco utilizando CDI
     * @return uma instância de um FileSystem de conexão com banco
     */
    public GridFS getFS(){
        return fsProvider.get();
    }
}
