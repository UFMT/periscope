package br.ufmt.periscope.repository;

import java.util.Collections;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import br.ufmt.periscope.model.ApplicantType;

import dev.morphia.Datastore;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Sort;

import static dev.morphia.query.filters.Filters.eq;

@ApplicationScoped
@Named
public class ApplicantTypeRepository {

//    private ResourceBundle bundle;
    private @Inject
    Datastore ds;

    @PostConstruct
    public void init() {
//        Locale locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
//        bundle = ResourceBundle.getBundle("messages", locale);
    }

    public void createIfNotExists(ApplicantType type) {
        long count = ds.find(ApplicantType.class)
                .filter(eq("name", type.getName()))
                .count();

        if (count > 0) {
            return;
        }

        ds.save(type);
    }

    public List<ApplicantType> getAll() {
        List<ApplicantType> ret = ds.find(ApplicantType.class)
                .iterator(new FindOptions().sort(Sort.ascending("name")))
                .toList();
        Collections.sort(ret);
        return ret;
    }

//    public ResourceBundle getBundle() {
//        return bundle;
//    }
//
//    public void setBundle(ResourceBundle bundle) {
//        this.bundle = bundle;
//    }
}
