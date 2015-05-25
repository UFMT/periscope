package br.ufmt.periscope.controller;

import br.ufmt.periscope.lazy.LazyPatentBrazilianDataModel;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.DataModel;
import javax.inject.Inject;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.qualifier.CurrentProject;

/**
 * - @ManagedBean<BR/>
 * - @ViewScoped<BR/>
 * Classe controller responsável por operações relacionadas às patentes brasileiras
 */
@ManagedBean
@ViewScoped
public class PatentBrazilianController {

    private @Inject
    @CurrentProject
    Project currentProject;
    private @Inject
    LazyPatentBrazilianDataModel patents;

    /**
     * Método pós construtor do controller<BR/>
     * Carrega as patentes brasileiras de maneira Lazy
     */
    @PostConstruct
    public void init() {
        patents.getRepo().setBlacklisted(false);
        patents.getRepo().setCompleted(true);
        patents.getRepo().setCurrentProject(currentProject);
    }

    /**
     *
     * @return
     */
    public DataModel<Patent> getPatents() {
        return patents;
    }

    /**
     *
     * @param patents
     */
    public void setPatents(DataModel<Patent> patents) {
        this.patents = (LazyPatentBrazilianDataModel) patents;
    }

    /**
     *
     * @return
     */
    public int getPartialCount() {
        return patents.getRowCount();
    }
}
