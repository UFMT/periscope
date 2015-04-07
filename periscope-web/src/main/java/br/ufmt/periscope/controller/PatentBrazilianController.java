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

@ManagedBean
@ViewScoped
public class PatentBrazilianController {

    private @Inject
    @CurrentProject
    Project currentProject;
    private @Inject
    LazyPatentBrazilianDataModel patents;

    /**
     * Controller constructor<BR/>
     * Loads project's brazilian patents
     *
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
