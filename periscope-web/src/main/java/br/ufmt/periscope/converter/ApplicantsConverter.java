/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufmt.periscope.converter;

import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.repository.ApplicantRepository;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author pc
 */
@Named
public class ApplicantsConverter implements Converter {

    @Inject
    private ApplicantRepository applicantRepository;

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String name) {
        Applicant applicant;
        System.out.println("dentro:"+name);
        applicant = applicantRepository.getApplicantByName(name);
        System.out.println("dentro2:"+applicant.getName());
        return applicant;

    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        Applicant applicant = (Applicant) value;
        return applicant.getName() + "";
    }
}
