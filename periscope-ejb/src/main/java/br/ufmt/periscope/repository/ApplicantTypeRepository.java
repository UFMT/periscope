package br.ufmt.periscope.repository;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import br.ufmt.periscope.model.ApplicantType;

import com.github.jmkgreen.morphia.Datastore;
import com.mongodb.BasicDBObject;
import java.util.Collections;
import java.util.Locale;
import java.util.ResourceBundle;
import javax.annotation.PostConstruct;
import javax.faces.context.FacesContext;

@Named
public class ApplicantTypeRepository {

    private ResourceBundle bundle;
    private @Inject
    Datastore ds;

    @PostConstruct
    public void init() {
        Locale locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
        bundle = ResourceBundle.getBundle("messages", locale);
    }

    public void createIfNotExists(ApplicantType type) {
        BasicDBObject where = new BasicDBObject("name", type.getName());
        int count = ds.getCollection(ApplicantType.class).find(where).count();

        if (count >= 0) {
            return;
        }

        ds.save(type);
    }

    public List<ApplicantType> getAll() {
        List<ApplicantType> ret = ds.createQuery(ApplicantType.class).order("name").asList();
        Collections.sort(ret);
        return ret;
    }

    public ResourceBundle getBundle() {
        return bundle;
    }

    public void setBundle(ResourceBundle bundle) {
        this.bundle = bundle;
    }
}
